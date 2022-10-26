package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AliPayService {
    /**
     * 生成二维码
     * @param orderId
     * @return
     */
    String createAliPay(Long orderId) throws AlipayApiException;

    /**
     * 退款接口
     * @param orderId
     * @return
     */
    Boolean refund(Long orderId);

    /**
     * 关闭支付宝交易
     * @param orderId
     * @return
     */
    Boolean closeAliPay(Long orderId);

    /**
     * 查询支付宝交易记录
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
