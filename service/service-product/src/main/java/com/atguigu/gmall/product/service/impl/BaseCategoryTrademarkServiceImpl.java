package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl  implements BaseCategoryTrademarkService {


    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public List<BaseTrademark> getFindTrademarkList(Long category3Id) {
        //  已知三级分类Id 获取 trademark_id ，再根据trademark_id 找 base_trademark
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id", category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        //判断
        if (!CollectionUtils.isEmpty(baseCategoryTrademarks)) {
            List<Long> collect = baseCategoryTrademarks.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectBatchIds(collect);
            return baseTrademarkList;
        } else {
            //返回集合数据
            return null;
        }

    }

    @Override
    public List<BaseTrademark> getCurrentTrademarkList(Long category3Id) {
        //  所有品牌：    baseTrademarkMapper.selectList(null);
        //  获取到当前分类下已绑定的 tmId  调用上面的方法 1,3
        //  排除：2,5,7
        //  已知三级分类Id 获取 trademark_id ，再根据trademark_id 找 base_trademark
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);
        //判断
        if (!CollectionUtils.isEmpty(baseCategoryTrademarks)){
            //获取已经绑定
            List<Long> collect = baseCategoryTrademarks.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            List<BaseTrademark> baseTrademarks = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !collect.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            //返回未绑定数据
            return baseTrademarks;
        }else {
            return baseTrademarkMapper.selectList(null);
        }

    }

    //根据实例对象保存
    @Override
    public void saveCategoryTrademark(CategoryTrademarkVo categoryTrademarkVo) {
        //  本质：保存数据到 base_category_trademark
        //  {category3Id: 63, trademarkIdList: [1, 2]}  63,1  63,2
        //  先获取到品牌Id 集合
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            //遍历集合
            trademarkIdList.forEach(trademarkId ->{
                //创建对象
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setTrademarkId(trademarkId);
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                //添加成功
                baseCategoryTrademarkMapper.insert(baseCategoryTrademark);
            });
        }
    }

    //根据id删除
    @Override
    public void removeById(Long category3Id, Long trademarkId) {
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        baseCategoryTrademarkQueryWrapper.eq("trademark_id",trademarkId);
        this.baseCategoryTrademarkMapper.delete(baseCategoryTrademarkQueryWrapper);
    }
}
