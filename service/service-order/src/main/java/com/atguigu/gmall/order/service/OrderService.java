package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    /**
     * 生成流水号
     *
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     *
     * @param tradeNo
     * @param userId
     * @return
     */
    Boolean checkTradeNo(String tradeNo, String userId);

    /**
     * 删除流水号
     *
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 校验库存
     *
     * @param skuId
     * @param skuNum
     * @return
     */
    Boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 保存订单
     *
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 查询分页数据
     *
     * @param orderInfoPage
     * @param userId
     * @return
     */
    IPage<OrderInfo> getOrderPage(Page<OrderInfo> orderInfoPage, String userId);

    /**
     * 取消订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单id 获取到对象
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 更新订单状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存系统
     * @param orderId
     */
    void sendOrderWare(Long orderId);

    /**
     * 获取子订单数据
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 将orderInfo转换为Map集合
     * @param orderInfo
     * @return
     */
    Map<String, Object> initWare(OrderInfo orderInfo);

    /**
     * 关闭过期订单
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
