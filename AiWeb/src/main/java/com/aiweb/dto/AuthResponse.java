package com.aiweb.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 在用户登录或认证成功后，向前端返回 JWT 令牌
 */
public class AuthResponse {
    private String token;

}
