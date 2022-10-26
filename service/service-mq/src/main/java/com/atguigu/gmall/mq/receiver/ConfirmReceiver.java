package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConfirmReceiver {

    // 监听消息绑定:启动时会初始化交换机队列
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routing.confirm"}
    ))
    public void getMsg(String msg, Message message, Channel channel) {
        //  消息队列中的数据.
        try {
            System.out.println("msg:\t" + msg);
            //  int i = 1/0;
            System.out.println(new String(message.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            //  异常消费的确认！ 第三个参数 重回队列！
            System.out.println("重回队列...");
            //  限制重回次数. 5 -- 使用redis 记录.
            //            if(count>5){
            //                return;
            //            }else {
            //                出现网络抖动的时候，业务上的异常，直接别写了!
            //                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            //                return;
            //            }
            //  直接记录消息表：table;  insert into tname values();
        }
        //  第一个相当于消息的标识,    第二个表示：是否批量确认，false: 单条 true: 批量.
        //  正常消费的确认!
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
