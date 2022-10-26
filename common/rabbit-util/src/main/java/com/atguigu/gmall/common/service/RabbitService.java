package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    // 封装发送消息的方法
    public Boolean sendMsg(String exchange, String routingKey, Object msg) {
        // 创建对象
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        // 赋值Id 数据 以后获取的时候根据这个key spring_returned_message_correlation 来获取correlationDatId
        String correlationDatId = UUID.randomUUID().toString();
        gmallCorrelationData.setId(correlationDatId);
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        // 重试的次数 初始化值 0 ， 让它累加
        redisTemplate.opsForValue().set(correlationDatId, JSON.toJSONString(gmallCorrelationData), 90, TimeUnit.SECONDS);
        // 调用发送消息的方法 ， 此时还用这个方法发送消息是否可以？ no ，需要将 gmallCorrelationData 发送出去
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, gmallCorrelationData);
        // 默认返回数据
        return true;
    }

}
