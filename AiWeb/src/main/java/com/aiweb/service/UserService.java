package com.aiweb.service;


import com.aiweb.dto.request.RegisterRequest;
import com.aiweb.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

//继承 UserDetailsService 以集成 Spring Security
public interface UserService extends UserDetailsService {
    /**
     * 用户注册
     * @param registerRequest
     */
    User register(RegisterRequest registerRequest);

    void loginUpdateTime(String username);

    public void forgertPassword(String userEmail);

    public void resetPassword(String token,String userEmail,String newPassword);

    public void checkUsernameAndSendCode(String username,String userEmail) throws Exception;

    /** 删除当前登录用户（含其在 FastGPT 上的专属数据集） */
    void deleteCurrentUser(String username);
    
    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);
}
