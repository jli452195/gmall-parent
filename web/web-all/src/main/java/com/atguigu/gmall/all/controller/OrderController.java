package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("trade.html")
    public String trade(Model model) {
        //  userAddressList detailArrayList totalNum  totalAmount
        Result<Map<String, Object>> result = orderFeignClient.trade();
        // 一起封装到map中
        model.addAllAttributes(result.getData());
        // 返回订单结算页
        return "order/trade";
    }

    //  http://order.gmall.com/myOrder.html
    @GetMapping("myOrder.html")
    public String myOrder() {
        //  页面加载的时候主动异步请求的是查询分页！找我的订单.
        //  返回我的定的页面。
        return "order/myOrder";
    }

}
