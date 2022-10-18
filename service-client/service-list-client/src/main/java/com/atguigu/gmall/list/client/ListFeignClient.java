package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-list", fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    //检索
    @PostMapping("api/list")
    Result search(@RequestBody SearchParam searchParam);

    //上架
    @GetMapping("api/list/inner/upperGoods/{skuId}")
    Result upperGoods(@PathVariable Long skuId);

    //下架
    @GetMapping("api/list/inner/lowerGoods/{skuId}")
    Result lowerGoods(@PathVariable Long skuId);

    //热度排名接口
    @GetMapping("api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable Long skuId);

}
