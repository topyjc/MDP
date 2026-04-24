package com.mdp.server.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JWTService {

    // 🚨 중요: 토큰에 도장을 찍을 비밀키입니다. (무조건 영문+숫자 32글자 이상이어야 안전합니다!)
    private final String SECRET_KEY = "MySuperSecretKeyForMdpProjectWhichIsVeryLongAndSecure";

    // 토큰 유효 기간: 24시간 (밀리초 단위)
    private final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    // 비밀키를 암호화 알고리즘에 맞는 형태(Key 객체)로 변환하는 메서드
    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 🔥 토큰 발급 메서드
    public String generateToken(String userId, int isAdmin) {
        return Jwts.builder()
                .setSubject(userId) // 토큰의 주인 (아이디)
                .claim("isAdmin", isAdmin) // 추가 데이터 (관리자 여부)
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 비밀키로 도장 쾅!
                .compact(); // 텍스트(String)로 압축해서 반환
    }
}