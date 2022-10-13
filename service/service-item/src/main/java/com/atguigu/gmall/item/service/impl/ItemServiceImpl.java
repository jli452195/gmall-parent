package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ItemServiceImpl implements ItemService {

    @Resource
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    //根据skuI获取数据 将获取存入map 返回
    @Override
    public Map getItemBySkuId(Long skuId) {
        Map<String, Object> map = new HashMap();
        // 调用service-product-client接口

        //判断
//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        if (!bloomFilter.contains(skuId)) {
//            // 不存在，则返回null集合
//            return new HashMap<>();
//        }
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 根据skuId获取数据
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            //返回数据
            return skuInfo;
        }, threadPoolExecutor);


        CompletableFuture<Void> spuPosterCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 根据spuId 获取海报信息
            List<SpuPoster> spuPosterList = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            map.put("spuPosterList", spuPosterList);
        }, threadPoolExecutor);

        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 获取分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        }, threadPoolExecutor);

        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            // 获取sku最新价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        }, threadPoolExecutor);

        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 根据skuId spuId获取销售属性
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        CompletableFuture<Void> skuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 切换商品
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String strJson = JSON.toJSONString(skuValueIdsMap);
            map.put("valuesSkuJson", strJson);
        }, threadPoolExecutor);


        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            // 通过skuId集合来查询数据
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            // 规格与包装：展示的时候需要展示的 平台属性名：平台属性值名  Thymeleaf ${attrName}:${attrValue}
            if (!CollectionUtils.isEmpty(attrList)) {
                List<HashMap<String, Object>> attrMapList = attrList.stream().map(baseAttrInfo -> {
                    //声明一个对象
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("attrName", baseAttrInfo.getAttrName());
                    hashMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                    return hashMap;
                }).collect(Collectors.toList());

                map.put("skuAttrList", attrMapList);
            }
        }, threadPoolExecutor);

        //多任务组合
        CompletableFuture.allOf(skuInfoCompletableFuture,
                spuPosterCompletableFuture,
                categoryViewCompletableFuture,
                priceCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuJsonCompletableFuture,
                attrCompletableFuture
        ).join();



        return map;
    }
}
