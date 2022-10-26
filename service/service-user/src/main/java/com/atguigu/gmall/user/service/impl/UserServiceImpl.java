package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name", userInfo.getLoginName());
        String passwd = userInfo.getPasswd();
        // 使用MD5对称加密
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfoQueryWrapper.eq("passwd", newPasswd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        // 判断
        if (info != null) {
            return info;
        }
        return null;
    }
}
