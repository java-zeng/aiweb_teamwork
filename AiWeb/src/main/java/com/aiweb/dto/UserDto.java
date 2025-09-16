package com.aiweb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * 一个安全的用户数据对象，用于向前端返回用户信息。关键在于它不包含密码等敏感字段。
 */
public class UserDto {
    private Long id;
    private String username;
    private String nickname;
    private String password;
    private String imageUrl;//存放的是用户头像对应的地址
    private Integer status;
}
