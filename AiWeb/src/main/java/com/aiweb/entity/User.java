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
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;
    private String password;//存储的加密之后的密码
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
