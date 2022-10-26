package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Service
public class AliPayServiceImpl implements AliPayService {

    @Resource
    private AlipayClient alipayClient;

    @Autowired
    private PaymentService paymentService;

    @Resource
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Override
    public Boolean checkPayment(Long orderId) {
        // 获取订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }


    }

    @Override
    public Boolean closeAliPay(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 声明对象
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Boolean refund(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        //  bizContent.put("trade_no", "2021081722001419121412730660"); // paymentInfo;
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            if (("Y").equals(response.getFundChange())) {
                // 关闭交易状态 ，订单状态
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
                paymentInfo.setUpdateTime(new Date());
                paymentService.updatePaymentStatus(paymentInfo, orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name());
                // 发送消息 给订单 更新状态
                return true;
            } else {
                return false;
            }
        } else {
            System.out.println("调用失败");
            return false;
        }
    }


    @Override
    public String createAliPay(Long orderId) throws AlipayApiException {
        // 根据订单id 获取订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        // 保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        // 判断
        if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())) {
            return "当前订单关闭或已支付";
        }

        // 创建Api对应的request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 设置同步回调地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步回调地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("total_amount", 0.01);
        bizContent.put("subject", "商品");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        // 过期时间 ：10分钟
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);
        // 设置成功
        bizContent.put("time_expire", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime()));
        alipayRequest.setBizContent(bizContent.toJSONString());
        // time_expire -- yyyy-MM-dd HH:mm:ss 订单绝对超时时间
        return alipayClient.pageExecute(alipayRequest).getBody();// 调用SDK生成表单
    }
}
