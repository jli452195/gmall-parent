package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/admin/product")
//@CrossOrigin
public class ManageController {

    @Resource
    private ManageService manageService;

    //admin/product/getCategory1
    @GetMapping("getCategory1")
    public Result getCategory1() {
        //调用服务层方法
        List<BaseCategory1> baseCategory1List = this.manageService.getCategory1();
        //返回所有的一级数据
        return Result.ok(baseCategory1List);
    }

    //admin/product/getCategory2/{category1Id}
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id) {
        //调用服务方法
        List<BaseCategory2> baseCategory2List = this.manageService.getCategory2(category1Id);
        //返回所有二级数据
        return Result.ok(baseCategory2List);
    }

    //admin/product/getCategory3/{category2Id}
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id) {
        //调用服务方法
        List<BaseCategory3> baseCategory3List = this.manageService.getCategory3(category2Id);
        //返回所有二级数据
        return Result.ok(baseCategory3List);
    }

    //admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id,
                                  @PathVariable Long category2Id,
                                  @PathVariable Long category3Id) {
        //  调用 服务层方法 平台属性 ： 平台属性值 1:n 返回1的哪一边，在一的哪一边配上一个集合！
        List<BaseAttrInfo> baseAttrInfoList = this.manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        //调用服务方法
        return Result.ok(baseAttrInfoList);
    }

    //  保存平台属性
    //  将Json 转换为java 能操作的 JavaObject
    //  @RequestBody  Json---> JavaObject
    //  @ResponseBody JavaObject ---> Json
    //admin/product/saveAttrInfo
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        //调用服务方法
        this.manageService.saveAttrInfo(baseAttrInfo);
        //返回确定
        return Result.ok();
    }

    //根据平台属性id 获取到平台属性值集合
    ///admin/product/getAttrValueList/{attrId}
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId) {
        //  调用服务层方法
        //  直接获取到属性值集合！select * from base_attr_value where attr_id = ?
//        List<BaseAttrValue> baseAttrValueList = this.manageService.getAttrValueList(attrId);
        //  select * from base_attr_info where id = attrId; 先走属性！再走属性值！
        BaseAttrInfo baseAttrInfo = this.manageService.getBaseAttrInfo(attrId);
        //返回数据
        return Result.ok(baseAttrInfo.getAttrValueList());
    }


}
