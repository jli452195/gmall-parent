package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Value("${authUrls.url}")
    private String authUrls; // authUrls = trade.html,myOrder.html,list.html

    @Autowired
    private RedisTemplate redisTemplate;

    // 创建一个匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 全局过滤方法
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取用户请求
        ServerHttpRequest request = exchange.getRequest();
        // 获取用户响应
        ServerHttpResponse response = exchange.getResponse();
        // 先获取用户url
        String path = request.getURI().getPath();
        // 判断
        if (antPathMatcher.match("/**/inner/**", path)) {
            // 属于访问内部数据接口
            // 提示没有访问权限
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 获取用户id
        String userId = this.getUserId(request);
        // 判断用户Id 是否合法
        if ("-1".equals(userId)) {
            // 非法请求
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 限制用户访问：/api/**/auth/*  判断用户是否登录
        if (antPathMatcher.match("/api/**/auth/*", path)) {
            // 只有未登录的情况下才提示信息
            if (StringUtils.isEmpty(userId)) {
                // 提示信息
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        // 提示用户信息需要登录， 访问web层的控制器
        //  authUrls = trade.html,myOrder.html,list.html
        String[] split = authUrls.split(",");
        if (split != null && split.length > 0) {
            for (String url : split) {
                //  0 trade.html 1 myOrder.html 2 list.html
                //  path http://list.gmall.com/list.html?category3Id=61
                // 你访问的url中包含要登录的控制器，但是你的用户id未空
                if (path.indexOf(url) != -1 && StringUtils.isEmpty(userId)) {
                    // 需要跳转 设置状态码
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    // 设置跳转的具体的url
                    //  http://list.gmall.com/list.html?category3Id=61
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://passport.gmall.com/login.html?originUrl=" + request.getURI());
                    // 返回
                    return response.setComplete();
                }
            }
        }

        // 将用户信息存储到请求头
        if (!StringUtils.isEmpty(userId)) {
            // 放入请求头 ServerHttpRequest request
            request.mutate().header("userId", userId).build();
            // request --> exchange
            return chain.filter(exchange.mutate().request(request).build());
        }

        // 默认返回
        return chain.filter(exchange);

    }

    /**
     * 获取用户Id
     *
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        // 重要的是获取到token cookie 或 请求头
        String token = "";
        // 从 cookie中获取数据
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        // 判断
        if (httpCookie != null) {
            token = httpCookie.getValue();
        } else {
            // 从请求头中获取
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)) {
                // 获取数据
                token = stringList.get(0);
            }
        }

        // 判断token
        if (!StringUtils.isEmpty(token)) {
            // 组成缓存的key
            String userKey = "user:login:" + token;
            // 根据key 来获取数据
            String userJson = (String) redisTemplate.opsForValue().get(userKey);
            // 判断
            if (!StringUtils.isEmpty(userJson)) {
                // 进行数据类型转换，并获取数据
                JSONObject userStr = JSONObject.parseObject(userJson);
                // 获取ip地址
                String ip = (String) userStr.get("ip");
                // 判断ip 地址是否一致
                if (ip.equals(IpUtil.getGatwayIpAddress(request))) {
                    // 获取到userId并返回
                    String userId = (String) userStr.get("userId");
                    return userId;
                } else {
                    // ip 地址不一致 返回-1
                    return "-1";
                }
            }
        }
        return "";
    }

    /**
     * 用户友好提示的方法
     *
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        // 输出的内容 resultCodeEnum.getMessage();
        Result<Object> result = Result.build(null, resultCodeEnum);
        // result 变为字符串
        String resultStr = JSON.toJSONString(result);
        // 将字符串变为字节数组，返回数据流
        DataBuffer wrap = response.bufferFactory().wrap(resultStr.getBytes());
        //  设置请求头格式：Content-Type = "text/html" ;"application/json;charset=utf8" uft-8;
        response.getHeaders().add("Content-Type", "application/json;charset=utf8");
        // 写数据
        return response.writeWith(Mono.just(wrap));
    }
}
