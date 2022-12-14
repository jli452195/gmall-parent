package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    //sku分页列表
    //admin/product/list/{page}/{limit}
    //http://localhost/admin/product/list/1/10?category3Id=61
    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SkuInfo skuInfo){
        //创建一个Page对象
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        //调用服务层方法
        IPage iPage = this.manageService.getSkuInfoList(skuInfoPage,skuInfo);
        //返回数据
        return Result.ok(iPage);
    }

    //保存sku属性
    //http://localhost/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        //调用服务层
        this.manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //下架商品信息
    ///admin/product/cancelSale/{skuId}
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //调用服务层
        this.manageService.cancelSale(skuId);
        return Result.ok();
    }


    //下架商品信息
    ///admin/product/onSale/{skuId}
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        //调用服务层
        this.manageService.onSale(skuId);
        return Result.ok();
    }





}
