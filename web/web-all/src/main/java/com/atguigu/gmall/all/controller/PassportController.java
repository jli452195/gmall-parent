package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {

    //  window.location.href = 'http://passport.gmall.com/login.html?originUrl='+window.location.href
    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        // 后台存储${originUrl} ,作用， 记录用户同哪里点击的登录url
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);
        return "login";
    }

}
