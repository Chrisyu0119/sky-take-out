package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌驗證的攔截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 驗證jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        System.out.println("當前線程的ID:" + Thread.currentThread().getId());

        //判斷目前攔截到的是Controller的方法還是其他資源
        if (!(handler instanceof HandlerMethod)) {
            //目前攔截到的不是動態方法，直接放行
            return true;
        }

        //1、從請求標頭中取得令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、驗證令牌
        try {
            log.info("jwt驗證:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("目前用戶id：{}", userId);
            BaseContext.setCurrentId(userId);
            //3、通過，放行
            return true;
        } catch (Exception ex) {
            //4、不通過，回應401狀態碼
            response.setStatus(401);
            return false;
        }
    }
}