package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/api/payment/alipay")
public class AliPayController {

    @Autowired
    private AliPayService aliPayService;

    @Autowired
    private PaymentService paymentService;

    @Value("${app_id}")
    private String app_id;

    @Autowired
    private RedisTemplate redisTemplate;


    //  http://api.gmall.com/api/payment/alipay/submit/74
    @SneakyThrows
    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String aLiPay(@PathVariable Long orderId) {

        // 调用服务层方法
        String from = null;
        try {
            from = aliPayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        // 返回数据
        return from;

    }

    // 同步回调
    // http://api.gmall.com/api/payment/alipay/callback/return
    @GetMapping("/callback/return")
    public String callbackReturn(HttpServletRequest request, @RequestParam HashMap<String, Object> map) {
        String outTradeNo = request.getParameter("out_trade_no");
        System.out.println("outTradeNo = " + outTradeNo);
        System.out.println("map:\t" + map);

        // 可以更新交易记录状态 PAID
        // http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;
    }

    // 异步通知
    //  http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
    //  https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    // 异步回调时支付宝主动发起的
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam HashMap<String, String> paramsMap) {
        System.out.println("异步回调......");
        // 获取到out_trade_no
        String outTradeNo = paramsMap.get("out_trade_no");
        String totalAmount = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");
        String tradeStatus = paramsMap.get("trade_status");
        // 过滤重复需要使用的
        String notifyId = paramsMap.get("notify_id");
        // Map<String,String> paramsMap = ... 将异步通知中收到的所有参数存放到Map中
        boolean signVerified = false; // 调用SDK签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }
        // 查询数据
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //  支付的时候是0.01 而表里的实际金额.
            if (paymentInfo == null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount)) != 0 || !app_id.equals(appId)
            ) {
                return "failure";
            }

            // 判断
            Boolean result = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 24 * 60 + 22, TimeUnit.MINUTES);
            if (!result) {
                return "failure";
            }
            // 校验状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 修改支付交易记录 后续会使用mq方式更新订单
                paymentService.updatePaymentInfoStatus(outTradeNo, PaymentType.ALIPAY.name(), paramsMap);
                return "success";
            }


        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
        }
        return "success";
    }

    // 退款接口
    @GetMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        // 调用服务层方法
        Boolean flag = aliPayService.refund(orderId);
        return Result.ok(flag);
    }

    // 关闭支付宝交易记录
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closeAliPay(@PathVariable Long orderId) {
        // 调用服务层
        Boolean flag = aliPayService.closeAliPay(orderId);
        // 返回数据
        return flag;
    }

    // 查询支付宝的交易记录
    @GetMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {
        // 调用服务层方法
        Boolean flag = aliPayService.checkPayment(orderId);
        // 返回数据
        return flag;

    }

    // 根据商户订单号 获取交易记录
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo) {
        // 调用服务层方法
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        return paymentInfo;
    }


}
