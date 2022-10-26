package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import io.swagger.annotations.ResponseHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        // 调用服务层方法
        UserInfo info = userService.login(userInfo);
        // 不执行
        // 判断
        if (info != null) {
            // 说明数据库有这个信息
            // 声明一个map集合
            HashMap<String, Object> hashMap = new HashMap<>();

            // 生成token , 存储到cookie , 传递到前端 , 前端会自动保存到cookie
            String token = UUID.randomUUID().toString();

            hashMap.put("token", token);
            // 登录成功以后 需要显示用户昵称 将这个用户昵称 放入map中
            hashMap.put("nickName", info.getNickName());

            // 需要将用户id 存储到缓存中 从服务器的角度来判断是否已经登录！
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            // 防止token被盗用 验证ip地址
            String ip = IpUtil.getIpAddress(request); // 登陆时获取到ip地址，存储到缓存中

            JSONObject userJson = new JSONObject();
            // 一定要变为字符串
            userJson.put("userId", info.getId().toString());
            userJson.put("ip", ip);
            redisTemplate.opsForValue().set(userKey, userJson.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            // 返回map集合
            return Result.ok(hashMap);
        } else {
            return Result.ok().message("用户名或密码不正确！");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request, @RequestHeader String token) {
        String token1 = request.getHeader("token");
        System.out.println("token1 = " + token1);
        // 清空cookie 数据 js 自动实现 ，以及redis数据
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        this.redisTemplate.delete(userKey);
        // 默认返回
        return Result.ok();
    }

}
