package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/item")
public class ItemApiController {

    //调用服务层
    @Resource
    private ItemService itemService;

    //  定好feignClient 的接口地址
    @GetMapping("{skuId}")
    public Result getItemBySkuId(@PathVariable Long skuId){
        //返回一个map
        Map map = this.itemService.getItemBySkuId(skuId);
        return Result.ok(map);
    }



}
