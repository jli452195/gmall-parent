package com.atguigu.gmall.payment.service;


import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.HashMap;

public interface PaymentService {

    /**
     * 保存支付交易记录
     * @param orderInfo
     * @param paymentType
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 根据第三方交易编号查询交易记录
     * @param outTradeNo
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 更新订单交易记录
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    void updatePaymentInfoStatus(String outTradeNo, String paymentType, HashMap<String, String> paramsMap);

    /**
     * 更新状态
     * @param paymentInfo
     * @param outTradeNo
     * @param paymentType
     */
    void updatePaymentStatus(PaymentInfo paymentInfo, String outTradeNo, String paymentType);
}
