package com.aiweb.service.impl;

import com.aiweb.dto.request.RegisterRequest;
import com.aiweb.entity.PasswordReset;
import com.aiweb.entity.User;
import com.aiweb.entity.Verification;
import com.aiweb.mapper.PasswordResetMapper;
import com.aiweb.mapper.UserMapper;
import com.aiweb.mapper.VerificationMapper;
import com.aiweb.service.EmailService;
import com.aiweb.service.UserService;
import com.aiweb.service.VerificationService;
import com.aiweb.utils.SmsUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationMapper verificationMapper;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private PasswordResetMapper passwordResetMapper;



    public void checkUsernameAndSendCode(String username,String userEmail) throws Exception {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            if (userMapper.selectOne(queryWrapper) != null) {
                throw new RuntimeException("用户名已存在");
            }
            String veriCode= smsUtil.getCode();
            String title="用户注册验证码";
            LocalDateTime expireTime =LocalDateTime.now().plusMinutes(5);
            verificationService.sendAndsaveVerification(userEmail,title,veriCode,expireTime);
    }

    @Override
    public User register(RegisterRequest registerRequest) {

        String userEmail=registerRequest.getEmail();
        String inputCode=registerRequest.getInputCode();
        
        // 检查输入验证码是否为空
        if (inputCode == null || inputCode.trim().isEmpty()) {
            throw new RuntimeException("验证码不能为空！");
        }

        boolean isCodeVaild=verificationService.checkCode(userEmail,inputCode);
        if(!isCodeVaild)
        {
            throw new RuntimeException("短信验证码输入错误！");
        }
        //当验证码输入正确时，也就是注册成功，我们把注册表中标注位设置成1
        QueryWrapper<Verification> queryWrapper1=new QueryWrapper<>();
        queryWrapper1.eq("user_email",registerRequest.getEmail());
        Verification verification =verificationMapper.selectOne(queryWrapper1);
        verification.setResult(true);
        verificationMapper.updateById(verification);
        //注册成功后，写入用户表中
        User user=new User();
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));//加密后存入对象
        user.setEmail(registerRequest.getEmail());
        user.setNickname(registerRequest.getNickname());
        userMapper.insert(user);
        return user;
    }

    @Override
    public void deleteCurrentUser(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在:" + username);
        }
        // 注意：删除用户时不再删除FastGPT数据集，避免循环依赖
        // 如果需要删除数据集，请手动处理
        userMapper.deleteById(user.getId());
    }
    //TODO 吧查询语句写在一个方法中：用用户名查询，返回查询语句
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
    
    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    @Override
    public void forgertPassword(String userEmail)
    {
        //1.校验user表中是否有该邮箱对应的用户
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("email",userEmail);
        User user=userMapper.selectOne(queryWrapper);
        //2.判断找到的用户是否非空
        if(user==null)
        {
            throw new RuntimeException("没有该用户，请重新注册");
        }
        //3.生成短期重置令牌,有效期30分钟
        String resetToken= java.util.UUID.randomUUID().toString();
        LocalDateTime expireTime=LocalDateTime.now().plusMinutes(30);
        //4.将要重置密码的用户数据以及令牌/有效时间，存入重置密码用户表password_reset中
        PasswordReset userPasswordReset=new PasswordReset();
        userPasswordReset.setUserEmail(userEmail);
        userPasswordReset.setResetByToken(resetToken);
        userPasswordReset.setUserId(user.getId());
        userPasswordReset.setExpireTime(expireTime);
        passwordResetMapper.insert(userPasswordReset);
        //4.构建重置链接
        String resetLink="http://localhost:3000/reset?token=" + resetToken + "&email=" + userEmail;
        //5.发送邮件
        String title="密码重置请求";
        String content="请点击以下链接重置您的密码(30分钟类有效):\n"+resetLink;
        emailService.sendEmail(userEmail,title,content);
    }

    @Override
    public void resetPassword(String token,String userEmail,String newPassword){
        //1.查询令牌记录
        QueryWrapper<PasswordReset> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("reset_by_token",token);
        queryWrapper.eq("user_email",userEmail);
        queryWrapper.eq("is_used",false);
        PasswordReset passwordResetByToken =passwordResetMapper.selectOne(queryWrapper);
        //2.校验令牌
        if(passwordResetByToken==null)
        {
            throw new RuntimeException("重置链接无效或者已被使用");
        }
        if(passwordResetByToken.getExpireTime().isBefore(LocalDateTime.now()))
        {
            throw new RuntimeException("链接已过期，请重新申请.");
        }
        //3.剩下的情况就是令牌有效
        User user=userMapper.selectById(passwordResetByToken.getUserId());
        if(user==null)
        {
            throw new RuntimeException("关联用户不存在.");
        }
        //4.加密新密码并更新
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        //5.使用过后，我们的令牌中is_used标为true
        passwordResetByToken.setIsUsed(true);
        passwordResetMapper.updateById(passwordResetByToken);

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
