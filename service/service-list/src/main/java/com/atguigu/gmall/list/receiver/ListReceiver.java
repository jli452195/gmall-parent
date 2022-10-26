package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    // 商品上架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) {
        // 判断
        try {
            if (skuId != null) {
                // 调用上架方法
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
            // 如果有错误  insert into table
        }
        // 手动签收 正确签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    // 商品下架
    @SneakyThrows
    @RabbitListener()
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        try {
            // 判断
            if (skuId != null) {
                // 调用上架的方法
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 手动签收 正确签收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

    }
}
