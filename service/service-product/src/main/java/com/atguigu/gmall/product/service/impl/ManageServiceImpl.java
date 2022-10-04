package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Resource
    private BaseCategory1Mapper baseCategory1Mapper;

    @Resource
    private BaseCategory2Mapper baseCategory2Mapper;

    @Resource
    private BaseCategory3Mapper baseCategory3Mapper;

    @Resource
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Resource
    private BaseAttrValueMapper baseAttrValueMapper;


    //直接获取到属性值集合
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //  直接获取到属性值集合！select * from base_attr_value where attr_id = ?
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(Long attrId) {
        //select * from base_attr_info where id = attrId
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            // select * from base_attr_value where attr_id = ?
            baseAttrInfo.setAttrValueList(this.getAttrValueList(attrId));
        }
        //返回数据
        return baseAttrInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        /*
        insert into tableName values();
        base_attr_info
        base_attr_value
         */
        if (baseAttrInfo.getId() != null) {
            //  修改操作 修改的平台属性名
            this.baseAttrInfoMapper.updateById(baseAttrInfo);
            //  修改平台属性值集合。要先删除，再新增！ 逻辑删除
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id", baseAttrInfo.getId());
            this.baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
        } else {
            //执行insert
            baseAttrInfoMapper.insert(baseAttrInfo);
        }

         /*
        insert into tableName values();
        base_attr_info
        base_attr_value
         */
        //获取数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //判断集合
        if (!CollectionUtils.isEmpty(attrValueList)) {
            attrValueList.forEach(baseAttrValue -> {
                //  平台属性值 -- 赋值一个attrId  base_attr_value.attr_id = base_attr_info.id
                //  获取到base_attr_info.id 主键
                //  type = IdType.AUTO 执行完插入之后，能够获取到主键自增！
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            });
        }

    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        //使用xml 多表查询
        return baseAttrInfoMapper.selectAttrInfoList(category1Id, category2Id, category3Id);
    }

    /**
     * 查询所有三级分类数据
     *
     * @param category2Id
     * @return
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //select * from base_category3 where category2_id = ? and is_deleted = 0;
        //wrapper
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id", category2Id);
        return baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
    }

    /**
     * 查询所有二级分类数据
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //select * from base_category2 where category1_id = ? and is_deleted = 0;
        //Wrapper = 构建查询条件 删除条件 修改条件 UpdateWrapper
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id", category1Id);
        return baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
    }

    /**
     * 查询所有一级分类数据
     *
     * @return
     */
    @Override
    public List<BaseCategory1> getCategory1() {
        //select * from base_category1;
        return baseCategory1Mapper.selectList(null);
    }
}
