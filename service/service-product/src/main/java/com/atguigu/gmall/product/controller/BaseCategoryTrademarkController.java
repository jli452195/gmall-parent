package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    //http://localhost/admin/product/baseCategoryTrademark/findTrademarkList/61
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result getFindTrademarkList(@PathVariable Long category3Id){
        //调用服务层
        List<BaseTrademark> baseTrademarkList = this.baseCategoryTrademarkService.getFindTrademarkList(category3Id);
        //返回数据
        return Result.ok(baseTrademarkList);
    }

    //http://localhost/admin/product/baseCategoryTrademark/findCurrentTrademarkList/61
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result getCurrentTrademarkList(@PathVariable Long category3Id){
        //调用服务层
        List<BaseTrademark> baseTrademarkList = this.baseCategoryTrademarkService.getCurrentTrademarkList(category3Id);
        //返回数据
        return Result.ok(baseTrademarkList);
    }

    //http://localhost/admin/product/baseCategoryTrademark/save
    @PostMapping("save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        //调用服务层
        this.baseCategoryTrademarkService.saveCategoryTrademark(categoryTrademarkVo);
        return Result.ok();
    }

    //admin/product/baseCategoryTrademark/remove/{category3Id}/{trademarkId}
    @DeleteMapping("remove/{category3Id}/{trademarkId}")
    public Result removeById(@PathVariable Long category3Id,
                             @PathVariable Long trademarkId){
        this.baseCategoryTrademarkService.removeById(category3Id,trademarkId);
        //返回
        return Result.ok();
    }


}
