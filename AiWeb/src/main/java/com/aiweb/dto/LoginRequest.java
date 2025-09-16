package com.aiweb.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

/**
 * 专门用于接收前端在登录时提交的 username 和 password
 */
public class LoginRequest {
    private String username;
    private String password;
}
