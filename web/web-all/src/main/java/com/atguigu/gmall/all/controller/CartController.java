package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Resource
    private ProductFeignClient productFeignClient;

    //  window.location.href = 'http://cart.gmall.com/addCart.html?skuId=' + this.skuId + '&skuNum=' + this.skuNum
    @GetMapping("addCart.html")
    public String addCart(HttpServletRequest request) {
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        // 获取skuInfo对象
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.valueOf(skuId));

        request.setAttribute("skuNum", skuNum);
        request.setAttribute("skuInfo", skuInfo);

        // 返回添加购物车页面
        return "cart/addCart";

    }

    // 购物车列表
    @GetMapping("/cart.html")
    public String cartList() {
        // 返回购物车列表
        return "cart/index";
    }

}
