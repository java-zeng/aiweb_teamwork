package com.aiweb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("verifications")
public class Verification {
    private Integer id;
    private String phoneNumber;
    private String verificationCode;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private Boolean result;
}
