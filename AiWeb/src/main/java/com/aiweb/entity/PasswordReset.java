package com.aiweb.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordReset {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Long userId;
    private String userEmail;
    private LocalDateTime expireTime;
    private String resetByToken;
    private Boolean isUsed;
}
