package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface ManageService {
    /**
     * 查询所有一级分类数据
     *
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 查询所有二级分类数据
     *
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 查询所有三级分类数据
     *
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id获取平台属性集合
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存-修改平台属性
     *
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id 获取到平台属性值集合
     *
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性id 获取到平台属性值
     *
     * @param attrId
     * @return
     */
    BaseAttrInfo getBaseAttrInfo(Long attrId);

    /**
     * spu分页列表
     *
     * @param spuInfoPage
     * @return
     */
    IPage getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    /**
     * 获取销售属性
     *
     * @param
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spu属性
     *
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * spu分页列表
     *
     * @param skuInfoPage
     * @param skuInfo
     * @return
     */
    IPage getSkuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo);

    /**
     * 根据spuId 查询销售属性
     *
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 根据spuId回显图片
     *
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 保存sku属性
     *
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 下架商品信息
     *
     * @param skuId
     */
    void cancelSale(Long skuId);


    /**
     * 上架商品信息
     *
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 根据skuId获取数据
     *
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);


    /**
     * 根据spuId获取海报信息
     *
     * @param spuId
     * @return
     */
    List<SpuPoster> getSpuPosterBySpuId(Long spuId);

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 获取sku最新价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据skuId spuId获取销售属性
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 切换商品
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     * 通过skuId集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
