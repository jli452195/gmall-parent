package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import feign.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    /**
     * 根据分类id 查询平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoList(@Param("category1Id") Long category1Id,@Param("category2Id") Long category2Id,@Param("category3Id") Long category3Id);

    /**
     * 通过skuId集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> selectBaseAttrInfoBySkuId(Long skuId);
}
