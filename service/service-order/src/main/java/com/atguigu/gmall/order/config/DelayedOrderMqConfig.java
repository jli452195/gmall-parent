package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DelayedOrderMqConfig {

    // 设置交换机
    @Bean
    public CustomExchange delayExchange() {
        // 声明一个map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, "x-delayed-message", true, false, map);
    }

    // 设置队列
    @Bean
    public Queue delayQueue() {
        //不需要设置额外的参数
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true, false, false);
    }

    // 设置绑定关系
    @Bean
    public Binding delayBinding() {
        // 返回数据
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
