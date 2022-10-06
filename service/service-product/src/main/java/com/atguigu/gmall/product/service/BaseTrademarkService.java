package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;


public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 获取品牌列表
     * @param baseTrademarkPage
     * @return
     */
    IPage getBaseTrademark(Page<BaseTrademark> baseTrademarkPage);

}
