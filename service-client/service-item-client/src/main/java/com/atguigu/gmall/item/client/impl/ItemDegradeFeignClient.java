package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

/**
 * author:atGuiGu-mqx
 * date:2022/10/7 10:42
 * 描述：
 **/
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {
    @Override
    public Result getItemBySkuId(Long skuId) {
        return null;
    }
}