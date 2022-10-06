package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SpuManageController {

    @Resource
    private ManageService manageService;

    //spu分页列表
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SpuInfo spuInfo) {
        //  mybatis-plus 里面有自带的分页类：Page 有一个接口IPage
        //  泛型：查询的是哪个表，泛型就写哪个实体类
        Page<SpuInfo> spuInfoPage = new Page<>(page, limit);
        //调用服务层
        IPage iPage = this.manageService.getSpuInfoList(spuInfoPage, spuInfo);
        //返回数据
        return Result.ok(iPage);
    }

    //获取销售属性
    //admin/product/baseSaleAttrList
    @GetMapping("baseSaleAttrList")
    public Result getBaseSaleAttrList(){
        List<BaseSaleAttr> baseAttrInfoList = this.manageService.getBaseSaleAttrList();

        return Result.ok(baseAttrInfoList);
    }

    //admin/product/saveSpuInfo
    //保存spu
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        //调用服务层
        this.manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    //根据spuId 查询销售属性
    //admin/product/spuSaleAttrList/{spuId}
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable Long spuId){
        //调用服务层
        List<SpuSaleAttr> spuSaleAttrList =  this.manageService.getSpuSaleAttrList(spuId);

        return Result.ok(spuSaleAttrList);
    }


    //根据spuId回显图片
    //http://localhost/admin/product/spuImageList/9
    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId){
        //调用服务层
        List<SpuImage> spuImageList = this.manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }



}
