package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    /**
     * 根据用户id 查询收货地址列表
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressListByUserId(String userId);
}
