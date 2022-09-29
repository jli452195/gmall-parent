package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class ManageController {

    @Resource
    private ManageService manageService;

    //admin/product/getCategory1
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //调用服务层方法
        List<BaseCategory1> baseCategory1List = this.manageService.getCategory1();
        //返回所有的一级数据
        return Result.ok(baseCategory1List);
    }

    //admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        //调用服务方法
        List<BaseCategory2> baseCategory2List = this.manageService.getCategory2(category1Id);
        //返回所有二级数据
        return Result.ok(baseCategory2List);
    }


}
