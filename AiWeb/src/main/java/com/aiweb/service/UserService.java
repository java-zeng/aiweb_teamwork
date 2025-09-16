package com.aiweb.service;


import com.aiweb.dto.RegisterRequest;
import com.aiweb.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

//继承 UserDetailsService 以集成 Spring Security
public interface UserService extends UserDetailsService {
    /**
     * 用户注册
     * @param registerRequest
     */
    User register(RegisterRequest registerRequest);

    /**
     * 根据用户名设置用户信息
     * @param username
     * @return
     */
}
