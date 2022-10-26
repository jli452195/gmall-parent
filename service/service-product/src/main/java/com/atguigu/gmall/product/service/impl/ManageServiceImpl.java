package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.product.mapper.SpuInfoMapper;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private SpuInfoMapper spuInfoMapper;

    @Resource
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Resource
    private SpuImageMapper spuImageMapper;

    @Resource
    private SpuPosterMapper spuPosterMapper;

    @Resource
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Resource
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Resource
    private SkuInfoMapper skuInfoMapper;

    @Resource
    private SkuImageMapper skuImageMapper;

    @Resource
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;

    @Resource
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;


    //直接获取到属性值集合
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //  直接获取到属性值集合！select * from base_attr_value where attr_id = ?
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
    }


    //获取sku基本信息和图片信息
    @Override
    @GmallCache(prefix = "sku:")
    public SkuInfo getSkuInfo(Long skuId) {
        //根据id获取skuInfo信息
        //获取skuImage插入到skuInfo中
        //select * from sku_info where id = skuId;
//        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
//        List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
//        skuInfo.setSkuImageList(skuImageList);
//        return skuInfo;
        return this.getSkuInfoDB(skuId);
        //返回数据

    }

    private SkuInfo getSkuInfoDB(Long skuId) {
        //select * from sku_info where id = skuId;
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
        // 解决空指针
        if (skuInfo != null) {
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }


    //根据skuId spuId获取销售属性
    @Override
    @GmallCache(prefix = "spuSaleAttr:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //调用mapper层
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);

    }

    @Override
    public void updateCategory3ById(BaseCategory3 baseCategory3) {
        //延迟双删
        this.redisTemplate.delete("index:[]");
        this.baseCategory3Mapper.updateById(baseCategory3);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.redisTemplate.delete("index:[]");

    }

    //查询分类数据
    @Override
    @GmallCache(prefix = "index:")
    public List<JSONObject> getCategoryList() {
        // 声明一个集合
        List<JSONObject> list = new ArrayList<>();
        // 查询所有分类数据
        List<BaseCategoryView> baseCategoryViewList = this.baseCategoryViewMapper.selectList(null);

        // 声明一个index
        int index = 1;
        // 先按照一级分类id分组
        // key = category1Id value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> categoryView1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = categoryView1Map.entrySet().iterator();
        while (iterator.hasNext()) {
            // 创建一个一级对象
            JSONObject category1 = new JSONObject();
            // 获取数据并赋值
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            Long category1Id = entry.getKey();
            List<BaseCategoryView> baseCategoryViewList1 = entry.getValue();
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            category1.put("categoryName", baseCategoryViewList1.get(0).getCategory1Name());

            // index:迭代
            index++;
            // 创建一个二级分类集合数据
            ArrayList<JSONObject> category2Child = new ArrayList<>();
            // 获取二级分类数据!
            Map<Long, List<BaseCategoryView>> categoryView2Map = baseCategoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = categoryView2Map.entrySet().iterator();
            while (iterator1.hasNext()) {
                //创建一个二级分类对象
                JSONObject category2 = new JSONObject();
                //获取数据并赋值
                Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
                Long category2Id = entry1.getKey();
                List<BaseCategoryView> baseCategoryViewList2 = entry1.getValue();
                category2.put("categoryId", category2Id);
                category2.put("categoryName", baseCategoryViewList2.get(0).getCategory2Name());

                // 获取三级分类数据
                List<JSONObject> category3Child = baseCategoryViewList2.stream().map(baseCategoryView -> {
                    //创建一个三级分类对象
                    JSONObject category3 = new JSONObject();
                    Long category3Id = baseCategoryView.getCategory3Id();
                    category3.put("categoryId", category3Id);
                    category3.put("categoryName", baseCategoryView.getCategory3Name());
                    return category3;
                }).collect(Collectors.toList());

                //将三级分类数据集合放入二级分类数据
                category2.put("categoryChild", category3Child);
                //将二级分类对象放入二级集合中
                category2Child.add(category2);
            }
            //将二级添加到一级分类中
            category1.put("categoryChild", category2Child);
            //将一级分类数据放入list集合中汇总
            list.add(category1);
        }
        return list;
    }

    //通过skuId集合来查询数据
    @Override
    @GmallCache(prefix = "attrList:")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return this.baseAttrInfoMapper.selectBaseAttrInfoBySkuId(skuId);
    }

    //切换商品
    @Override
    @GmallCache(prefix = "skuValue:")
    public Map getSkuValueIdsMap(Long spuId) {
        Map map = new HashMap();
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIds(spuId);
        if (!CollectionUtils.isEmpty(mapList)) {
            mapList.forEach(map1 -> {
                //{"3739|3741":27,"3738|3741":28}
                map.put(map1.get("value_ids"), map1.get("sku_id"));
            });
        }
        //返回数据
        return map;
    }

    //获取sku最新价格
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        //获取价格数据
        //select price from sku_info where id = ?
        SkuInfo skuInfo = skuInfoMapper.selectOne(new QueryWrapper<SkuInfo>().eq("id", skuId).select("price"));
        if (skuInfo != null) {
            return skuInfo.getPrice();
        }
        return null;
    }

    //通过三级分类id查询分类信息
    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        //select * from base_category_view where id = ?;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    //根据spuId获取海报信息
    @Override
    @GmallCache(prefix = "spuPoster:")
    public List<SpuPoster> getSpuPosterBySpuId(Long spuId) {
        //select * from spu_poster where spu_id = ? ;
        return spuPosterMapper.selectList(new QueryWrapper<SpuPoster>().eq("spu_id", spuId));
    }


    @Override
    public void onSale(Long skuId) {
        //isSale = 1
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);

        // 发送消息  发送消息内容 根据消费者的需求！ -- 调用方法的参数
        rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
    }

    //下架商品信息
    @Override
    public void cancelSale(Long skuId) {
        //  is_sale = 0
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
        //  发送消息.   发送消息内容 根据消费者的需求！ -- 调用方法的参数.
        this.rabbitService.sendMsg(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        this.skuInfoMapper.insert(skuInfo);

        //skuAttrValueList
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            skuAttrValueList.forEach(skuAttrValue -> {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            });
        }

        //sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.forEach(skuImage -> {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            });
        }

        //sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }

        // 保存时，将skuid添加到布隆过滤器
        RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.add(skuInfo.getId());

    }

    //根据spuId回显图片
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id", spuId));
    }

    //根据spuId 查询销售属性
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        // 本质查询数据 调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    //spu分页列表
    @Override
    public IPage getSkuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("category3_id", skuInfo.getCategory3Id());
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage, skuInfoQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
        spuInfo
        spuImage
        spuPoster	海报
        spuSaleAttr	销售属性
        spuSaleAttrValue	销售属性值
         */
        this.spuInfoMapper.insert(spuInfo);

        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        //  获取商品图片列表
        if (!CollectionUtils.isEmpty(spuImageList)) {
            spuImageList.forEach(spuImage -> {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }

        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        //  获取海报信息
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            spuPosterList.forEach(spuPoster -> {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            });
        }

        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        //  获取到销售属性
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //获取销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                spuSaleAttrValueList.forEach(spuSaleAttrValue -> {
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                    spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                });

            });
        }
    }

    //获取销售属性
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return this.baseSaleAttrMapper.selectList(null);
    }

    //spu分页列表
    @Override
    public IPage getSpuInfoList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        //  select * from spu_info where category3_id = 61 order by id desc limit 3,3 ;
        //  构建条件： category3_id = 61 order by id
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        return this.spuInfoMapper.selectPage(spuInfoPage, spuInfoQueryWrapper);
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
