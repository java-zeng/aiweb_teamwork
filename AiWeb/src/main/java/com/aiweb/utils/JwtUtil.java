package com.aiweb.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration}")
    private Long expire;

    /**
     * 新版本的密钥需要是SecreKey格式，我们需要把字符串->字符数组->SecreKey格式
     * @return
     */
    private SecretKey getSignKey(){
        //将字符串转换成字节数组
        byte[] bytes=secretString.getBytes(StandardCharsets.UTF_8);
        //使用 Keys.hmacShaKeyFor 方法根据字节数组生成一个安全的、不可变的 SecretKey
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 生成JWT令牌
     * @param claims JWT第二部分负载 payload 中存储的内容
     * @return
     */
    public String generateJwt(Map<String, Object> claims){
        String jwt = Jwts.builder()//构造器创建
                .claims(claims)//传入有效载荷
                .expiration(new Date(System.currentTimeMillis()+expire))//设置过期时间
                .signWith(getSignKey())//传入密钥
                .compact();//拼接好（头部，载荷，签名）
        return jwt;
    }

    /**
     * 解析JWT令牌
     * @param jwt JWT令牌
     * @return JWT第二部分负载 payload 中存储的内容
     */
    public Claims parseJWT(String jwt){
        Claims claims = Jwts.parser()//解析器创建
                .verifyWith(getSignKey())//传入密钥
                .build()//解析器创建完毕
                .parseSignedClaims(jwt)//验证令牌并解析出（头部，载荷）
                .getPayload();//获取有效载荷Payload
        return claims;
    }

    /**
     * 从jwt令牌中提取出用户名
     * @param token
     * @return
     */
    public String extractUsername(String token)
    {
        Claims claims=parseJWT(token);
        return claims.get("username",String.class);//把获取到的值强制转换为 String 类型。
    }

    /**
     * 从令牌中提取过期时间
     * @param token
     * @return
     */
    public Date extractExpiration(String token){
        return parseJWT(token).getExpiration();
    }

    /**
     * 判断令牌是否过期了
     * @param token
     * @return
     */
    public Boolean isTokenExpired(String token)
    {
       //判断令牌中提取的过期时间，是否早于当前时间？
        return extractExpiration(token).before(new Date());//new Date()默认会创建一个当前时间
    }

    /**
     * 这里就是验证令牌的有效性
     * @param token
     * @param userDetails
     * @return
     */
    public Boolean validateToken(String token, UserDetails userDetails)
    {
        final String username=extractUsername(token);
        //当令牌中提取出来的用户名和数据库中的用户名相同且令牌没有过期时，令牌是有效的
        return (username.equals(userDetails.getUsername())&&!isTokenExpired(token));
    }
}
