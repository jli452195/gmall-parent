package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private CartFeignClient cartFeignClient;

    @Resource
    private OrderService orderService;

    @Resource
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request) {
        // 从请求头中获取数据
        String userId = AuthContextHolder.getUserId(request);

        // userAddressList detailArrayList totalNum  totalAmount
        HashMap<String, Object> hashMap = new HashMap<>();

        // 获取用户收货地址
        Result result = userFeignClient.findUserAddressListByUserId(userId);
        // 获取到订单详细
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        // 创建一个对象
        AtomicInteger totalNum = new AtomicInteger();
        List<OrderDetail> detailArrayList = cartCheckedList.stream().map(cartInfo -> {
            // 创建订单明细对象
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            // 计算总件数
            totalNum.addAndGet(orderDetail.getSkuNum());

            return orderDetail;
        }).collect(Collectors.toList());

        // 总金额 = 单价*数量
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        hashMap.put("userAddressList", result.getData());
        hashMap.put("detailArrayList", detailArrayList);
        hashMap.put("totalNum", totalNum);
        hashMap.put("totalAmount", orderInfo.getTotalAmount());

        // 存储流水号
        hashMap.put("tradeNo", orderService.getTradeNo(userId));
        return Result.ok(hashMap);
    }

    // 保存订单
    //  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
    //  将json 转换为javaObject
    @PostMapping("auth/submitOrder")
    public Result saveOrderInfo(@RequestBody OrderInfo orderInfo,
                                HttpServletRequest request) {
        // 获取用户id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.valueOf(userId));
        // 判断 先页面提交的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 比较
        Boolean result = orderService.checkTradeNo(tradeNo, userId);  //N
        if (!result) {
            return Result.fail().message("不能重复无刷新回退提交订单");
        }

        // 删除流水号
        orderService.delTradeNo(userId); //N

        // 实现远程调用：库存系统接口
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        // 准备一个
        ArrayList<CompletableFuture> completableFutureArrayList = new ArrayList<>();
        // 定义一个集合来存储错误信息
        ArrayList<String> errorList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            // 循环订单明细
            for (OrderDetail orderDetail : orderDetailList) {
                // 调用验证接口
                CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                    Boolean exist = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum()); //N
                    if (!exist) {
                        // 没有库存
                        errorList.add(orderDetail.getSkuId() + ":库存不足。");
                    }
                }, threadPoolExecutor);
                // 将这个异步编排对象放到集合中
                completableFutureArrayList.add(stockCompletableFuture);
                // 校验价格
                CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                    // 获取实时价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    // 订单价格
                    BigDecimal orderPrice = orderDetail.getOrderPrice();
                    // 判断
                    if (skuPrice.compareTo(orderPrice) != 0) {
                        // 价格有变动
                        String msg = skuPrice.compareTo(orderPrice) == 1 ? "涨价" : "降价";
                        // 需要更新购物车价格
                        String userCartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(userCartKey, orderDetail.getSkuId().toString());
                        cartInfo.setSkuPrice(skuPrice);
                        redisTemplate.opsForHash().put(userCartKey, orderDetail.getSkuId().toString(), cartInfo);
                        // 返回提示信息
                        errorList.add(orderDetail.getSkuId() + msg + skuPrice.subtract(orderPrice.abs()));
                    }
                }, threadPoolExecutor);
                // 将验证价格的异步编排对象放入集合
                completableFutureArrayList.add(priceCompletableFuture);
            }
        }
        // 多任务组合
        CompletableFuture.allOf(completableFutureArrayList.toArray(new CompletableFuture[completableFutureArrayList.size()])).join();
        // 判断是否有错误，集合长度 size() ,数组长度 length ， 字符串长度 length(), 文件长度 length
        if (errorList.size() > 0) {
            return Result.fail().message(StringUtils.join(errorList, ","));
        }
        // 调用服务层方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        // 返回订单id
        return Result.ok(orderId);
    }

    // 查看我的订单
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrderPage(@PathVariable Long page,
                               @PathVariable Long limit,
                               HttpServletRequest request) {
        // 获取用户id
        String userId = AuthContextHolder.getUserId(request);
        // 分页-构建-一个page对象
        Page<OrderInfo> orderInfoPage = new Page<>(page, limit);
        // 调用服务层方法
        IPage<OrderInfo> iPage = orderService.getOrderPage(orderInfoPage, userId);
        // 返回数据
        return Result.ok(iPage);
    }

    // 根据orderId 获取到订单对象
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    // 拆单接口
    @PostMapping("orderSplit")
    public List<Map<String, Object>> orderSplit(HttpServletRequest request) {
        // 获取参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 获取到子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId, wareSkuMap);
        // 遍历循环
        List<Map<String, Object>> listMap = subOrderInfoList.stream().map(orderInfo -> {
            // 将这个ordeInfo变为map;
            Map<String, Object> map = orderService.initWare(orderInfo);
            return map;
        }).collect(Collectors.toList());
        // 返回数据
        return listMap;

    }

}
