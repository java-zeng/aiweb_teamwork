package com.aiweb.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 专门用于接收前端在注册时提交的用户信息
 */
public class RegisterRequest {
    private String username;
    private String nickname;
    private String password;
    private String phoneNumber;
    private String inputCode;//用户输入的验证码
    private String email;
}
