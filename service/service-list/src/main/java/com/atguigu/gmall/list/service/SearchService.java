package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

public interface SearchService {
    /**
     * 上架
     *
     * @param skuId
     */
    void upperGoods(Long skuId);


    /**
     * 下架
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 根据热度排名
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 查询数据
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam);

}
