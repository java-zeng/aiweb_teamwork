package com.aiweb.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 选择角色请求DTO
 */
@Data
public class SelectRoleRequest {
    
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
}
