package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/10/7 10:51
 * 描述：
 **/
@Controller // 返回页面
public class ItemController {

    @Resource
    private ItemFeignClient itemFeignClient;

    //  http://item.gmall.com/23.html
    @GetMapping("{skuId}.html")
    public String skuIdItem(@PathVariable Long skuId, Model model) {
        //  获取到商品详情渲染数据
        Result<Map> result = itemFeignClient.getItemBySkuId(skuId);
        //  存储数据
        model.addAllAttributes(result.getData());
        //  返回视图
        return "item/item";
    }

}