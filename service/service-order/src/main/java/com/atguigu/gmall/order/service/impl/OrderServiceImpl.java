package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl; // wareUrl=http://localhost:9001

    @Override
    public String getTradeNo(String userId) {
        // 生成一个流水号
        String tradeNo = UUID.randomUUID().toString();
        // 存储到缓存
        String tradeNoKey = "tradeNo:" + userId;
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;


    }

    @Override
    public void execExpiredOrder(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        // 发个消息异步
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        // 获取数据
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo != null) {
            // 查询订单明细
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
            orderInfo.setOrderDetailList(orderDetailList);
        }

        // 返回数据
        return orderInfo;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        // 声明子订单集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
            1. 获取到原始订单
            2. wareSkuMap 这个参数转换为java对象
            3. 创建子订单 ， 给子订单赋值 ， 并保存子订单
            4. 将子订单对象添加到集合中 并返回
            5. 修改原始订单状态
         */
        OrderInfo originOrderInfo = getOrderInfo(Long.valueOf(orderId));
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(mapList)) {
            // 循环遍历
            for (Map map : mapList) {
                String wareId = (String) map.get("wareId");
                List<String> skuIdList = (List<String>) map.get("skuIds");
                // 创建子订单
                OrderInfo subOrderInfo = new OrderInfo();
                BeanUtils.copyProperties(originOrderInfo, subOrderInfo);
                // 设置子订单id为null
                subOrderInfo.setId(null);
                subOrderInfo.setParentOrderId(Long.valueOf(orderId));
                // 细节仓库id
                subOrderInfo.setWareId(wareId);

                // 计算价格！ -- 跟明细有关系！
                List<OrderDetail> orderDetailArrayList = originOrderInfo.getOrderDetailList().stream().filter(orderDetail -> {
                    return skuIdList.contains(orderDetail.getSkuId().toString());
                }).collect(Collectors.toList());
                // 赋值子订单明细
                subOrderInfo.setOrderDetailList(orderDetailArrayList);
                subOrderInfo.sumTotalAmount();
                // 保存子订单
                saveOrderInfo(subOrderInfo);
                // 添加到子订单集合
                subOrderInfoList.add(subOrderInfo);

            }
            // 修改原始订单状态
            updateOrderStatus(Long.valueOf(orderId), ProcessStatus.SPLIT);
        }
        // 返回数据
        return subOrderInfoList;

    }

    @Override
    public void sendOrderWare(Long orderId) {
        // 更新订单在线状态
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        // 查询订单数据
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将订单中需要的业务字段 ，封装到一个map中
        Map<String, Object> map = initWare(orderInfo);

        // 发送消息
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, JSON.toJSONString(map));
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        // 更新
        // 优化关闭订单
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        // flag 为 2 的时候 关闭paymentInfo
        if ("2".equals(flag)) {
            rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE, MqConst.ROUTING_PAYMENT_CLOSE, orderId);
        }

    }

    /**
     * 将订单中需要的业务字段 封装到一个map中
     *
     * @param orderInfo
     * @return
     */
    public Map<String, Object> initWare(OrderInfo orderInfo) {
        // 声明一个map集合
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！

        // 获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 声明一个集合
        List<Map<String, Object>> orderDetailArrayList = orderDetailList.stream().map(orderDetail -> {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            detailMap.put("skuName", orderDetail.getSkuName());
            // 返回数据
            return detailMap;
        }).collect(Collectors.toList());
        map.put("details", orderDetailArrayList);
        return map;
    }

    // 更新订单
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        // 订单状态
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setUpdateTime(new Date());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId) {
        // 调用mapper
        IPage<OrderInfo> iPage = orderInfoMapper.selectOrderPage(orderInfoPage, userId);
        iPage.getRecords().stream().forEach(orderInfo -> {
            // 获取到订单状态名称
            String statusName = OrderStatus.getStatusNameByStatus(orderInfo.getOrderStatus());
            // 赋值状态名称
            orderInfo.setOrderStatusName(statusName);
        });
        return iPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //  total_amount,order_status,user_id,out_trade_no,trade_body,operate_time,expire_time,process_status
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //  outTradeNo = 后续作为商品的订单号使用，要求不能重复！
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + new Random().nextInt(10000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setTradeBody("毕业礼物"); // 可以获取商品的名称，细节需要注意商品名称的长度别超过数据库的字段长度
        orderInfo.setOperateTime(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //  插入order_info
        orderInfoMapper.insert(orderInfo);
        //  保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            orderDetailList.stream().forEach(orderDetail -> {
                //  赋值order_id
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            });
        }
        //  获取订单Id
        Long orderId = orderInfo.getId();
        //  返回
        return orderId;
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        // 使用httpclient 远程调用 -- 硬编码！ wareUrl=http://localhost:9001
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        // 返回判断结果
        return "1".equals(result);

    }

    @Override
    public void delTradeNo(String userId) {
        // 删除流水号
        String tradeNoKey = "tradeNo:" + userId;
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public Boolean checkTradeNo(String tradeNo, String userId) {
        // 获取缓存数据
        String tradeNoKey = "tradeNo:" + userId;
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        // 返回比较结果
        return tradeNo.equals(redisTradeNo);
    }
}
