package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

/**
 * author:atGuiGu-mqx
 * date:2022/9/30 11:37
 * 描述：
 **/
@Configuration
public class CorsConfig {

    //  配置webFilter
    @Bean
    public WebFilter webFilter() {
        //  创建对象 CorsConfiguration
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //  设置规则：
        corsConfiguration.addAllowedOrigin("*");    //  设置域名
        corsConfiguration.addAllowedMethod("*");    //  设置请求方法
        corsConfiguration.addAllowedHeader("*");    //  设置请求头
        corsConfiguration.setAllowCredentials(true);    //  允许携带请求头
        //  创建对象 UrlBasedCorsConfigurationSource
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}