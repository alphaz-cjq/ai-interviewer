package com.cjq.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/*
* JWT拦截器
*    令牌生成与校验
*    secret 和 expiration 从 application.yaml 读取，不再硬编码
* */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    /**
     * 获取密钥
     * 将字符串转换成JJWT需要的SecretKey对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    //生成JWT令牌
    public String generateJwt(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        Date expirationDate = new Date(now + expirationMillis);

        return Jwts.builder()
                .addClaims(claims)
                .expiration(expirationDate)
                .signWith(getSecretKey())
                .compact();
    }

    //解析并验证JWT令牌
    public Claims parseJwt(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();// 获取令牌的Payload部分
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException("令牌已过期");
        } catch (JwtException e) {
            throw new JwtValidationException("令牌验证失败");
        }
    }

    /**
     * JWT 过期异常（便于调用方区分过期 vs 篡改）
     */
    public static class JwtExpiredException extends RuntimeException {
        public JwtExpiredException(String message) {
            super(message);
        }
    }

    /**
     * JWT 验证失败异常
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }
    }
}
