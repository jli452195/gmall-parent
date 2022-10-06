package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //http://localhost/admin/product/baseTrademark/1/10
    @GetMapping("{page}/{limit}")
    public Result getBaseTrademark(@PathVariable Long page,
                                   @PathVariable Long limit){
        //调用服务层
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,limit);
        IPage iPage = this.baseTrademarkService.getBaseTrademark(baseTrademarkPage);
        //返回数据
        return Result.ok(iPage);
    }

//    http://localhost/admin/product/baseTrademark/save
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        //调用保存方法
        this.baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //http://localhost/admin/product/baseTrademark/remove/5
    @DeleteMapping("remove/{id}")
    public Result delete(@PathVariable Long id){
        //调用删除方法
        this.baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //  根据品牌Id 获取品牌对象
    @GetMapping("get/{id}")
    public Result getById(@PathVariable Long id){
        //调用服务层方法
        BaseTrademark byId = this.baseTrademarkService.getById(id);
        return Result.ok(byId);
    }

//    http://localhost/admin/product/baseTrademark/update
    //修改品牌
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        //调用服务层方法
        this.baseTrademarkService.updateById(baseTrademark);
        return  Result.ok();
    }


}
