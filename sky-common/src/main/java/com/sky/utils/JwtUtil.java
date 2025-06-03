package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    /**
     * 產生jwt
     * 使用Hs256演算法, 私鑰使用固定金鑰
     *
     * @param secretKey jwt金鑰
     * @param ttlMillis jwt過期時間(毫秒)
     * @param claims    設定的資訊
     * @return
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        // 指定簽章的時候使用的簽章演算法，也就是header那部分
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // 產生JWT的時間
        long expMillis = System.currentTimeMillis() + ttlMillis;
        Date exp = new Date(expMillis);

        // 設定jwt的body
        JwtBuilder builder = Jwts.builder()
                // 如果有私有宣告，一定要先設定這個自己建立的私有的宣告，這個是給builder的claim賦值，一旦寫在標準的宣告賦值之後，就是覆蓋了那些標準的宣告的
                .setClaims(claims)
                // 設定簽章使用的簽章演算法和簽章使用的金鑰
                .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8))
                // 設定過期時間
                .setExpiration(exp);

        return builder.compact();
    }

    /**
     * Token解密
     *
     * @param secretKey jwt金鑰 此金鑰一定要保留好在伺服器端, 不能暴露出去, 否則sign就可以被偽造, 如果對接多個客戶端建議改造成多個
     * @param token     加密後的token
     * @return
     */
    public static Claims parseJWT(String secretKey, String token) {
        // 得到DefaultJwtParser
        Claims claims = Jwts.parser()
                // 設定簽章的金鑰
                .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                // 設定需要解析的jwt
                .parseClaimsJws(token).getBody();
        return claims;
    }

}