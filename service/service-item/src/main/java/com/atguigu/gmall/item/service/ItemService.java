package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {
    /**
     * 根据skuId 获取到数据  将数据封装到Map中返回
     * @param skuId
     * @return
     */
    Map getItemBySkuId(Long skuId);
}
