package com.aiweb.controller;

import com.aiweb.common.Result;
import com.aiweb.dto.*;
import com.aiweb.service.EmailService;
import com.aiweb.service.VerificationService;
import com.aiweb.utils.JwtUtil;
import com.aiweb.entity.User;
import com.aiweb.service.UserService;
import org.bouncycastle.tsp.ers.ERSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private EmailService emailService;


    @PostMapping("/sendCode")
    public Result sendCode(@RequestBody RegisterRequest request)
    {
        try {
            userService.checkUsernameAndSendCode(request.getUsername(),request.getEmail());
            return Result.success("发送验证码成功!");
        }catch (Exception e)
        {
            return Result.error("用户名重复或发送邮件失败！,原因如下:"+e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest registerRequest){
        User user=userService.register(registerRequest);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest loginRequest){
        try{
            //1.调用 Spring Security 的认证管理器执行认证
            authenticationManager.authenticate
                    (new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),loginRequest.getPassword()));
            //接受前端传入的账号和明文密码，封装成UsernamePasswordAuthenticationToken对象，调用authenticationManager.authenticate方法,
            // 由authenticate() 调用 loadUserByUsername(),将用户数据返回成带加密密码的UserDetails格式，再由authenticate进行验证(用passwordEncoder)
        }catch (Exception e){
            //2.如果认证失败，抛出异常，返回错误信息
            return Result.error("用户名或者密码错误！");
        }
        userService.loginUpdateTime(loginRequest.getUsername());
        //3.生成准备放入jwt令牌中的信息
        Map<String,Object> claims=new HashMap<>();
        claims.put("username",loginRequest.getUsername());
        //4.生成jwt令牌,生成一个包含用户名的 JWT
        final String jwt=jwtUtil.generateJwt(claims);
        //5.把生成的jwt令牌返回给前端
        return Result.success(new AuthResponse(jwt));
    }

    @PostMapping("/forgot_password")
    public Result forgetPassword(@RequestBody ForgotPasswordRequest passwordRequest)
    {
        String email =passwordRequest.getUserEmail();
        try{
            userService.forgertPassword(email);
            return Result.success("重置链接已经发送到你的邮箱");
        }catch (Exception e)
        {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset_password")
    public Result resetPassword(@RequestBody ResetPasswordRequest request)
    {
        try{
            userService.resetPassword(request.getToken(),request.getUserEmail(),request.getNewPassword());
            return Result.success("重置密码成功，请重新登陆！");
        }catch (Exception e)
        {
            return Result.error(e.getMessage());
        }
    }
}

