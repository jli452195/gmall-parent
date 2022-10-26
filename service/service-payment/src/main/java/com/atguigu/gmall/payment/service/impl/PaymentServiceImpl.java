package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    // 保存支付交易记录
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        // 先查询数据
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfoQuery != null) {
            return;
        }

        // 创建 paymentInfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        // paymentInfo.setTradeNo();支付宝交易标号
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());

        // 交易记录
        paymentInfoMapper.insert(paymentInfo);

    }


    @Override
    public void updatePaymentInfoStatus(String outTradeNo, String paymentType, HashMap<String, String> paramsMap) {

        try {
            // 查询paymentInfo 中的orderId
            PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, paymentType);
            if (paymentInfoQuery == null) {
                return;
            }
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(paramsMap.toString());

            // 更新数据
            updatePaymentStatus(paymentInfo, outTradeNo, paymentType);

            // 发送消息给订单
            rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY, MqConst.ROUTING_PAYMENT_PAY, paymentInfoQuery.getOrderId());
        } catch (Exception e) {
            redisTemplate.delete(paramsMap.get("notify_id"));
            throw new RuntimeException(e);
        }

    }

    /**
     * 更新状态
     *
     * @param paymentInfo
     * @param outTradeNo
     * @param paymentType
     */
    public void updatePaymentStatus(PaymentInfo paymentInfo, String outTradeNo, String paymentType) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        paymentInfoMapper.update(paymentInfo, paymentInfoQueryWrapper);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        // 查询
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }
}
