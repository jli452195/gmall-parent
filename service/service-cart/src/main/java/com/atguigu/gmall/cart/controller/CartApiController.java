package com.atguigu.gmall.cart.controller;


import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {


    @Autowired
    private CartService cartService;

    // 添加购物车
    //  http://api.gmall.com/api/cart/addToCart/26/1
    @GetMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {

        // 获取用户id,添加购物车：登录、未登录都可以！ 网关鉴权的时候，将用户id 存储到header中！
        String userId = AuthContextHolder.getUserId(request);
        // 判断
        if (StringUtils.isEmpty(userId)) {
            // 说明未登录 -- 构建临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }

        // 调用服务层方法
        cartService.addToCart(skuId, userId, skuNum);

        // 默认返回
        return Result.ok();

    }

    // 查看购物车列表
    @GetMapping("cartList")
    public Result getCartList(HttpServletRequest request) {
        // 获取userId
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户id
        String userTempId = AuthContextHolder.getUserTempId(request);
        // 调用服务层方法
        List<CartInfo> cartInfoList = cartService.getCartList(userId, userTempId);
        // 返回购物车集合列表
        return Result.ok(cartInfoList);

    }

    // 修改状态
    //  this.api_name + '/checkCart/' + skuId + '/' + isChecked,
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request) {
        // 获取用户id、添加 购物车：登录、为登录都可以！ 网关鉴权的时候、将用户id放到header中
        String userId = AuthContextHolder.getUserId(request);
        // 判断
        if (StringUtils.isEmpty(userId)) {
            // 说明未登录 -- 构建用户临时id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用服务层的方法
        cartService.checkCart(skuId, userId, isChecked);

        // 返回购物车集合列表
        return Result.ok();
    }

    // 删除购物车url: this.api_name + '/deleteCart/' + skuId,
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request) {
        // 获取用户id、添加 购物车：登录、为登录都可以！ 网关鉴权的时候、将用户id放到header中
        String userId = AuthContextHolder.getUserId(request);
        // 判断
        if (StringUtils.isEmpty(userId)) {
            // 说明未登录 -- 构建临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用服务层方法
        cartService.deleteCart(skuId, userId);

        // 返回购物车集合列表
        return Result.ok();
    }

    // 获取购物车选中的商品
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        // 调用服务层方法
        return cartService.getCartCheckedList(userId);
    }


}
