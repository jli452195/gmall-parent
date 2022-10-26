package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://payment.gmall.com/pay.html?orderId=74
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request) {
        // 获取到订单id
        String orderId = request.getParameter("orderId");
        // 获取到订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.valueOf(orderId));
        //  后台存储orderInfo ;
        request.setAttribute("orderInfo", orderInfo);
        // 结算界面
        return "payment/pay";

    }

    // pay/success.html
    @GetMapping("pay/success.html")
    public String paySuccess() {
        // 返回成功界面
        return "payment/success";
    }

}
