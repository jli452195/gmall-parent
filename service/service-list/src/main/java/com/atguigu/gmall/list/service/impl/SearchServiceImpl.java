package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Resource
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    //上架
    @Override
    public void upperGoods(Long skuId) {
        // 声明对象
        Goods goods = new Goods();
        // 赋值Goods
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        goods.setId(skuId);
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());

        // price 查询数据库
        goods.setPrice(productFeignClient.getSkuPrice(skuId).doubleValue());
        goods.setCreateTime(new Date());

        // 品牌数据
        BaseTrademark baseTrademark = productFeignClient.getBaseTradeMarkById(skuInfo.getTmId());
        goods.setTmId(baseTrademark.getId());
        goods.setTmName(baseTrademark.getTmName());
        goods.setTmLogoUrl(baseTrademark.getLogoUrl());

        //分类数据
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());

        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());

        //平台属性 平台属性值
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            //平台属性id
            searchAttr.setAttrId(baseAttrInfo.getId());
            //平台属性名称
            searchAttr.setAttrName(baseAttrInfo.getAttrValueList().get(0).getValueName());
            // 平台属性名
            searchAttr.setAttrValue(baseAttrInfo.getAttrName());

            return searchAttr;
        }).collect(Collectors.toList());

        goods.setAttrs(searchAttrList);
        //保存数据
        goodsRepository.save(goods);

    }

    //查询数据
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        //创建对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
         * 1.生成dsl语句
         * 2.执行dsl语句
         *
         * 3.将赋值结果给 SearchResponseVo 对象
         */
        // 生成dsl语句
        SearchRequest searchRequest = this.queryBuildDsl(searchParam);

        // 执行dsl语句
        SearchResponse searchResponse = null;
        try {
            searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //数据结果集转换
        searchResponseVo = this.parseResultResponseVo(searchResponse);
        /*
        private List<SearchResponseTmVo> trademarkList;
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        private List<Goods> goodsList = new ArrayList<>();
        private Long total;//总记录数
        以上四个字段，在其他方法parseResultResponseVo中赋值

        以下三个字段，在此search方法赋值。
        private Integer pageSize;//每页显示的内容
        private Integer pageNo;//当前页面
        private Long totalPages;
         */
        searchResponseVo.setPageSize(searchParam.getPageSize());
        searchResponseVo.setPageNo(searchParam.getPageNo());

        long totalPages = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPages);
        // 返回对象
        return searchResponseVo;


    }

    // 生成dsl语句
    private SearchRequest queryBuildDsl(SearchParam searchParam) {
        // 创建一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //  创建QueryBuilder { query bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //  判断 第一个入口：是否根据分类Id 进行过滤.
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }

        // 第二个入口：全文检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            // 分词之后的 and 关系
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND));

            // 设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title");
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");

            // 将设置好的对象放入查询器
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        // 可能通过品牌过滤
        // trademark = 3 ; 华为
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            // 进行分割
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                // tmId = split[0]
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }

        // 平台属性-平台属性值进行过滤，因为数据类型有点不同，nested
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                // props=23:
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    // split [0]
                    // split [1]
                    // split [2]
                    // 创建中间层的bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 创建内存的bool
                    BoolQueryBuilder innerBoolQuery = QueryBuilders.boolQuery();
                    innerBoolQuery.must(QueryBuilders.matchQuery("attrs.attrId", split[0]));
                    innerBoolQuery.must(QueryBuilders.matchQuery("attrs.attrValue", split[1]));

                    boolQuery.must(QueryBuilders.nestedQuery("attrs", innerBoolQuery, ScoreMode.None));
                    // 将中间层放入外层
                    boolQueryBuilder.filter(boolQuery);
                    //  只有一层
                    //  boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs",innerBoolQuery,ScoreMode.None));
                }
            }
        }
        // { query bool }
        searchSourceBuilder.query(boolQueryBuilder);

        // 分页，排序，聚合
        // 从第二条数据开始查看
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        // 排序：
        // order = 1:sac order = 1:desc 综合热度 order = 2:asc order=2:desc 价格
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            // : 分割
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                // split[0];split[1]
                String field = "";
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }

        } else {
            // 默认排序规则
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        }

        // 设置聚合： 去重显示品牌 与 平台属性属性信息
        // 品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"))
        );

        // 平台属性集合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")))
        );

        // 创建一个SearchRequest对象
        // GET /goods/_search
        SearchRequest searchRequest = new SearchRequest("goods");

        // 查看dsl语句
        System.out.println("dsl:\t" + searchSourceBuilder.toString());
        // 将dsl语句 封装到查询请求对象中
        searchRequest.source(searchSourceBuilder);

        // 指定哪些字段是对程序有用的，哪些字段是没有用的！
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price", "createTime"}, null);
        // 返回对象
        return searchRequest;

    }

    // 数据结果集转换
    private SearchResponseVo parseResultResponseVo(SearchResponse searchResponse) {
        // 创建一个对象 SearchResponseVo
        SearchResponseVo searchResponseVo = new SearchResponseVo();
         /*
        private List<SearchResponseTmVo> trademarkList;
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        private List<Goods> goodsList = new ArrayList<>();
        private Long total;//总记录数
        以上四个字段，在其他方法parseResultResponseVo中赋值
         */
        SearchHits hits = searchResponse.getHits();
        // 获取记录总数
        searchResponseVo.setTotal(hits.getTotalHits().value);
        SearchHit[] subHits = hits.getHits();
        // 声明一个goodList集合
        List<Goods> goodsList = new ArrayList<>();
        if (subHits != null && subHits.length > 0) {
            // 循环遍历
            for (SearchHit subHit : subHits) {
                // 获取到json字符串
                String sourceAsString = subHit.getSourceAsString();
                // json字符串是good类型的
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);
                // 判断是否根据全文检索 如果是全文检索，则获取高亮的title
                if (subHit.getHighlightFields().get("title") != null) {
                    // 说明显示有高亮
                    String title = subHit.getHighlightFields().get("title").getFragments()[0].toString();
                    goods.setTitle(title);
                }
                goodsList.add(goods);
            }
        }

        // 设置商品集合
        searchResponseVo.setGoodsList(goodsList);

        // 获取品牌 -- 从聚合
        Map<String, Aggregation> stringAggregationMap = searchResponse.getAggregations().asMap();
        // 获取返回数据
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) stringAggregationMap.get("tmIdAgg");
        // 获取品牌数据
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 获取品牌id
            String tmId = bucket.getKeyAsString();
            searchResponseTmVo.setTmId(Long.valueOf(tmId));
            // 获取品牌的name
            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            // 只有一个品牌名，所以写0
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 获取品牌的logoUrl
            ParsedStringTerms tmLogoUrlAgg = bucket.getAggregations().get("tmLogoUrlAgg");
            // 只有一个品牌名所以写0
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            // 返回对象
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        // 添加品牌数据
        searchResponseVo.setTrademarkList(trademarkList);

        // 获取平台属性数据：从聚合中获取
        ParsedNested attrsAgg = (ParsedNested) stringAggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            // 创建一个平台属性
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            // 获取到平台属性Id
            String attrId = bucket.getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.valueOf(attrId));

            // 获取平台属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            // 获取平台属性值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            // 获取平台属性值名称
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());

            searchResponseAttrVo.setAttrValueList(attrValueList);
            // 返回数据
            return searchResponseAttrVo;
        }).collect(Collectors.toList());

        // 将集合添加到平台属性中
        searchResponseVo.setAttrsList(attrsList);
        // 返回数据
        return searchResponseVo;

    }

    // 热度排名
    @Override
    public void incrHotScore(Long skuId) {
        // 借助 redis数据类型 以及key ZSet
        String hotKey = "hotScore";
        Double aDouble = redisTemplate.opsForZSet().incrementScore(hotKey, "hotScore:" + skuId, 1);
        //判断
        if (aDouble % 10 == 0) {
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(aDouble.longValue());
            goodsRepository.save(goods);
        }
    }

    //下架
    @Override
    public void lowerGoods(Long skuId) {
        // 删除数据
        this.goodsRepository.deleteById(skuId);
    }
}
