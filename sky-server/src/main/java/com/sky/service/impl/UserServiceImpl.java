package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    //微信服務接口URL
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登錄
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //這段抽取到下面寫成方法
//        //調用微信接口服務,獲取用戶OpenID
//        Map<String, String> map = new HashMap<>();
//        map.put("appid", weChatProperties.getAppid());
//        map.put("secret", weChatProperties.getSecret());
//        map.put("js_code", userLoginDTO.getCode());
//        map.put("grant_type", "authorization_code");
//        String json = HttpClientUtil.doGet(WX_LOGIN, map);
//        JSONObject jsonObject = JSON.parseObject(json);
//        String openid = jsonObject.getString("openid");

        //變成
        String openid = getOpenid(userLoginDTO.getCode());

        //判斷OpenID是否為空,為空登錄失敗,拋出業務異常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判斷當前微信用戶是否為新用戶
        User user = userMapper.getByOpenid(openid);
        //如果為新用戶,自動完成註冊
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回用戶對象
        return user;
    }

    /**
     * 調用微信接口服務,獲取用戶OpenID
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        //調用微信接口服務,獲取用戶OpenID
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
