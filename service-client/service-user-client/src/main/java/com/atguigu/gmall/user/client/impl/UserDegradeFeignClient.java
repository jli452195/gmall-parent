package com.atguigu.gmall.user.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.stereotype.Component;

@Component
public class UserDegradeFeignClient implements UserFeignClient {
    @Override
    public Result findUserAddressListByUserId(String userId) {
        return null;
    }
}
