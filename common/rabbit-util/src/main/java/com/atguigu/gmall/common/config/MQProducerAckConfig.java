package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@Log4j2
public class MQProducerAckConfig implements RabbitTemplate.ReturnCallback, RabbitTemplate.ConfirmCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    // 初始化
    @PostConstruct
    public void init() {
        rabbitTemplate.setReturnCallback(this);
        rabbitTemplate.setConfirmCallback(this);
    }


    /**
     * 判断消息是否到交换机
     *
     * @param correlationData correlation data for the callback.
     * @param ack             true for ack, false for nack
     * @param cause           An optional cause, for nack, when available, otherwise null.
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            // 消息到了交换机
            System.out.println("消息到了......");
        } else {
            // 消息未到交换机
            log.error("消息未到交换机......");
            // 调用回调方法
            retrySendMsg(correlationData);
        }
    }


    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        // 表示消息未到队列
        String correlationDatId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        // 从缓存获取数据
        String strJson = (String) redisTemplate.opsForValue().get(correlationDatId);
        // 获取到 gmallCorrelationData
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(strJson, GmallCorrelationData.class);
        // 调用重试方法
        retrySendMsg(gmallCorrelationData);

    }

    // 编写重试方法
    private void retrySendMsg(CorrelationData correlationData) {
        // 获取到重试的次数
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        // 获取到当前的重试此数值，初始化值 0
        int retryCount = gmallCorrelationData.getRetryCount();
        // 判断 0 1 2
        if (retryCount > 2) {
            // 2 以上，重试次数已到，但是还没有发送出去
            // 消息记录表，哪个消息未发送出去， 可以根据这个消息的重要级别 1: 必须 0: 非必须
            return;
        } else {
            // 0 1 2
            // 重试次数进行累加
            retryCount++;
            // 将这个重试次数写redis
            gmallCorrelationData.setRetryCount(retryCount);
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSON(gmallCorrelationData), 90, TimeUnit.SECONDS);
            // 重试次数
            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
        }
    }
}
