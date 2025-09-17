package com.aiweb.service.impl;

import com.aiweb.dto.RegisterRequest;
import com.aiweb.entity.User;
import com.aiweb.mapper.UserMapper;
import com.aiweb.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.authentication.AnonymousAuthenticationWebFilter;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 注册的逻辑：用户名存在：报错；不存在：用户名存入User，密码加密后存入
     * @param registerRequest
     */
    @Override
    public User register(RegisterRequest registerRequest) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", registerRequest.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user=new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));//加密后存入对象
        user.setEmail(registerRequest.getEmail());
        user.setNickname(registerRequest.getNickname());
        userMapper.insert(user);
        return user;
    }

    @Override
    public void loginUpdateTime(String username) {
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("username",username);
        User user =userMapper.selectOne(queryWrapper);
        if(user!=null)
        {
            user.setLastLogin(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {//若为null则表示没查到
            throw new UsernameNotFoundException("用户名不存在:" + username);
        }
        //将用户数据封装成SpringSecurity要求的UserDetails格式
        return org.springframework.security.core.userdetails.User//这里的User不是我们创建的实体类，而是SpringSecurity中的范例，泛指用户数据
                .withUsername(user.getUsername())//设置用户名
                .password(user.getPassword())//设置密码,(这里的密码必须是加密后的)
                .authorities("ROLE_USER")//设置用户权限或角色
                .build();
    }
}
