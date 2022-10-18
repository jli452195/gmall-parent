package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {

    @Resource
    private ListFeignClient listFeignClient;

    @GetMapping("list.html")
    public String list(Model model, SearchParam searchParam) {
        // 后台需要存储数据
        // searchParam , trademarkParam -- 品牌面包屑 , propsParamList -- 平台属性面包屑 ,urlParam -- 记录原始查询条件 ,orderMap -- 排序规则
        // listfeign远程调用. 传递多个数据，需要将这个参数变为 json 对象在feign 中传递.
        Result<Map> result = listFeignClient.search(searchParam);
        // 制作面包屑： 品牌小米
        String trademarkParam = this.makeTradeMarkParam(searchParam.getTrademark());
        List<SearchAttr> searchAttrList = this.makeAttrList(searchParam.getProps());
        Map<String, Object> orderMap = this.makeOrderMap(searchParam.getOrder());
        // 制作urlParam
        String urlParam = this.makeUrlParam(searchParam);
        //  trademarkList,attrsList,goodsList,pageNo,totalPages 其实都是实体类的属性.SearchResponseVo
        model.addAllAttributes(result.getData());
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("trademarkParam", trademarkParam);
        model.addAttribute("propsParamList", searchAttrList);
        model.addAttribute("orderMap", orderMap);
        // 返回检索界面
        return "list/index";

    }

    // 制作urlParam
    private String makeUrlParam(SearchParam searchParam) {
        // 字符串拼接
        StringBuilder stringBuilder = new StringBuilder(); // 不安全快
        // 通过关键词 keyword手机
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            stringBuilder.append("keyword=").append(searchParam.getKeyword());
        }
        // 可能通过分类检索 category3Id= 61
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            stringBuilder.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            stringBuilder.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            stringBuilder.append("category1Id=").append(searchParam.getCategory1Id());
        }

        // 根据品牌检索 category3Id=61&trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("&trademark=").append(searchParam.getTrademark());
            }
        }

        // 根据平台检索 category3Id=61&trademark=1:小米&props=23:8G:运行内存&props=24:128G:机身内存
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("&props=").append(prop);
                }
            }
        }

        // 返回数据
        return "list.html?" + stringBuilder.toString();


    }

    // 排序
    private Map<String, Object> makeOrderMap(String order) {
        // 创建一个map对象
        Map<String, Object> map = new HashMap<>();
        // 判断
        if (!StringUtils.isEmpty(order)) {
            // order=2:desc
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                map.put("type", split[0]);
                map.put("sort", split[1]);
            }
        } else {
            map.put("type", "1");
            map.put("sort", "desc");
        }
        return map;
    }

    // 平台属性面包屑
    private List<SearchAttr> makeAttrList(String[] props) {
        List<SearchAttr> searchAttrList = new ArrayList<>();
        // 平台属性名:平台属性值名
        if (props != null && props.length > 0) {
            // 遍历这个数组 24:128G:机身内存
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    SearchAttr searchAttr = new SearchAttr();
                    searchAttr.setAttrId(Long.valueOf(split[0]));
                    searchAttr.setAttrValue(split[1]);
                    searchAttr.setAttrName(split[2]);
                    searchAttrList.add(searchAttr);
                }
            }

        }

        //返回数据
        return searchAttrList;
    }

    // 品牌面包屑
    private String makeTradeMarkParam(String trademark) {
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                return "品牌：" + split[1];
            }
        }
        return null;

    }
}
