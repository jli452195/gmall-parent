package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;

import java.util.List;

public interface BaseCategoryTrademarkService {
    /**
     * 根据category3Id获取可选品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getFindTrademarkList(Long category3Id);

    /**
     * 根据category3Id保存
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getCurrentTrademarkList(Long category3Id);

    /**
     * 根据实体对象保存
     * @param categoryTrademarkVo
     */
    void saveCategoryTrademark(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 根据id删除
     * @param category3Id
     * @param trademarkId
     */
    void removeById(Long category3Id, Long trademarkId);
}
