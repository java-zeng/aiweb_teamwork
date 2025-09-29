package com.aiweb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    //校验用户的有效性
    private String token;
    //确定用户的身份
    private String userEmail;
    //确定新的密码
    private String newPassword;
}
