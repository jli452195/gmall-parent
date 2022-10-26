package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Resource
    private PaymentFeignClient paymentFeignClient;


    // 取消订单
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrder(Long orderId, Message message, Channel channel) {

        try {
            // 判断
            if (orderId != null) {
                // 查询状态！  状态未支付！UNPAID ，取消订单
                OrderInfo orderInfo = orderService.getById(orderId);
                // 判断
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())) {
                    // 判断是否有电商本地交易记录
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    // 判断
                    if (paymentInfo != null && "UNPAID".equals(paymentInfo.getPaymentStatus())) {
                        // 是否关闭支付宝的交易记录
                        Boolean result = paymentFeignClient.checkPayment(orderId);
                        if (result) {
                            // result = true 有交易记录
                            // 有可能会关闭支付的交易记录 什么时候不能关闭
                            Boolean flag = paymentFeignClient.closeAliPay(orderId); // 支付宝本地的交易记录
                            if (flag) {
                                // true ; 未付款
                                orderService.execExpiredOrder(orderId, "2");
                            } else {
                                //  付款了。不关闭了，会自动实现异步回调....
                            }
                        } else {
                            // 没有交易记录
                            // 有交易记录 有可能 关闭orderInfo + paymentInfo
                            orderService.execExpiredOrder(orderId, "2");
                        }
                    } else {
                        // 只有orderInfo
                        // 取消订单
                        orderService.execExpiredOrder(orderId, "1");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }

    // 监听支付发送的消息更新订单状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId, Message message, Channel channel) {
        try {
            // 判断
            if (orderId != null) {
                // 修改状态
                // 查询状态
                OrderInfo orderInfo = orderService.getById(orderId);
                if ("UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())) {
                    // 调用更新方法
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                    // 发送消息给库存
                    orderService.sendOrderWare(orderId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 监听减库存结果：
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void stock(String jsonStr, Message message, Channel channel) {
        try {
            // 判断
            if (!StringUtils.isEmpty(jsonStr)) {
                // 从json字符串中获取到 orderId , status {"orderId":1,"status":"DEDUCTED"}
                Map map = JSON.parseObject(jsonStr, Map.class);
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");

                if ("DEDUCTED".equals(status)) {
                    // 说明减库存成功，更新状态
                    orderService.updateOrderStatus(Long.valueOf(orderId), ProcessStatus.WAITING_DELEVER);
                } else {
                    // 减库存失败
                    orderService.updateOrderStatus(Long.valueOf(orderId), ProcessStatus.STOCK_EXCEPTION);
                    // 补货，人工顾客介入 补货成功之后，发送一个消息更新当前订单状态
                }
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
