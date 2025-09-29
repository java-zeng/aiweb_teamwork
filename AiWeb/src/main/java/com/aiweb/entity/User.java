package com.aiweb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 引入 TableField
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("users")
public class User {

    // 1. ID: BIGINT UNSIGNED 对应 Long 类型
    @TableId(type = IdType.AUTO)
    private Long id; // 修正：使用 Long 而不是 Integer

    // 2. 核心登录凭证
    private String username;
    private String email; // 新增：对应 email 字段
    private String password;
    private String phoneNumber;

    // 3. 用户公开资料
    private String nickname; // 新增：对应 nickname 字段

    // 4. 驼峰命名映射与 TableField
    // MyBatis-Plus 默认将 image_url 映射到 imageUrl，如果需要精确控制，可使用 @TableField
    @TableField("image_url")
    private String imageUrl; // 新增：对应 image_url 字段 (Java 习惯使用驼峰命名)

    // 5. 账户状态: TINYINT UNSIGNED 对应 Integer 或 Byte
    private Integer status; // 新增：对应 status 字段

    // 6. 审计与追踪
    // TIMESTAMP NULL DEFAULT NULL 建议使用 LocalDateTime 或 Timestamp
    @TableField("last_login")
    private LocalDateTime lastLogin; // 新增：对应 last_login 字段

    // 7. 时间字段 (MyBatis-Plus 默认开启驼峰映射，无需 @TableField，但写上更清晰)
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    // 8. 软删除字段
    @TableField("delete_time")
    private LocalDateTime deleteTime; // 新增：对应 delete_time 字段
}