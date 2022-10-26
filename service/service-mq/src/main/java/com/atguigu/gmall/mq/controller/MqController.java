package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @GetMapping("sendMsg")
    public Result sendMsg() {
        // 发送消息
        rabbitService.sendMsg("exchange.confirm", "routing.confirm", "死鬼，才回来.");
        // 默认返回
        return Result.ok();
    }

}
