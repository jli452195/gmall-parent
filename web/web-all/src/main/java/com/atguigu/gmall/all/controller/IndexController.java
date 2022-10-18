package com.atguigu.gmall.all.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Controller
public class IndexController {

    @Resource
    private ProductFeignClient productFeignClient;

    //模板引擎对象
    @Autowired
    private TemplateEngine templateEngine;

    //首页数据展示控制器、
    @GetMapping({"/", "index.html"})
    public String index(Model model) {
        // 获取首页分类数据
        List<JSONObject> list = productFeignClient.getBaseCategoryList();
        //存储数据
        model.addAttribute("list", list);
        //返回页面
        return "index/index";
    }

    // 创建一个静态化界面
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        // 获取到静态界面需要渲染的数据
        List<JSONObject> list = productFeignClient.getBaseCategoryList();
        // 第一个参数 模板名 ， 第二季参数  输出对象 写对象
        Context context = new Context();
        context.setVariable("list",list);
        //创建一个写对象
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        templateEngine.process("index/index.html",context,fileWriter);
        // 默认返回
        return Result.ok();
    }


}
