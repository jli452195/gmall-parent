package com.atguigu.gmall.cart.service;


import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 查看购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 修改状态
     * @param skuId
     * @param userId
     * @param isChecked
     */
    void checkCart(Long skuId, String userId, Integer isChecked);

    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 查询这个用户选中的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
