package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;

import java.util.List;

public interface ManageService {
    /**
     * 查询所有一级分类数据
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 查询所有二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 查询所有三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类id获取平台属性集合
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存-修改平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id 获取到平台属性值集合
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性id 获取到平台属性值
     * @param attrId
     * @return
     */
    BaseAttrInfo getBaseAttrInfo(Long attrId);
}
