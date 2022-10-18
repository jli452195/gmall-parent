package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Resource
    private SearchService searchService;


    // 创建es映射
    @GetMapping("inner/createIndex")
    public Result createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    //上架
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId) {
        //调用服务层方法
        this.searchService.upperGoods(skuId);
        return Result.ok();
    }

    //下架
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId) {
        //调用服务层方法
        this.searchService.lowerGoods(skuId);
        return Result.ok();
    }

    //热度排名接口
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId) {
        this.searchService.incrHotScore(skuId);
        return Result.ok();
    }

    @PostMapping
    public Result search(@RequestBody SearchParam searchParam){
        //查询数据
        SearchResponseVo searchResponseVo = this.searchService.search(searchParam);
        return Result.ok(searchResponseVo);
    }
}
