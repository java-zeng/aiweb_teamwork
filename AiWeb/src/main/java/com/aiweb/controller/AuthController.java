package com.aiweb.controller;

import com.aiweb.common.Result;
import com.aiweb.dto.AuthResponse;
import com.aiweb.dto.LoginRequest;
import com.aiweb.dto.RegisterRequest;
import com.aiweb.utils.JwtUtil;
import com.aiweb.entity.User;
import com.aiweb.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest registerRequest){
        try{
            userService.register(registerRequest);
            return Result.success("注册成功！");
        }catch(RuntimeException e){
            return Result.error(e.getMessage());
        }
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
        //3.生成准备放入jwt令牌中的信息
        Map<String , Object> claims=new HashMap<>();
        claims.put("username",loginRequest.getUsername());
        //4.生成jwt令牌,生成一个包含用户名的 JWT
        final String jwt=jwtUtil.generateJwt(claims);
        //5.把生成的jwt令牌返回给前端
        return Result.success(new AuthResponse(jwt));
    }

}
