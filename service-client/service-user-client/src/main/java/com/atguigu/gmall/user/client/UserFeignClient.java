package com.atguigu.gmall.user.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    /**
     * 根据userId查询收获地址列表
     * @param userId
     * @return
     */

    @GetMapping("api/user/inner/findUserAddressListByUserId/{userId}")
    Result findUserAddressListByUserId(@PathVariable String userId);

}
