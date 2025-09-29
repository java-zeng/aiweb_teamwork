package com.aiweb.service.impl;

import com.aiweb.entity.User;
import com.aiweb.entity.Verification;
import com.aiweb.mapper.VerificationMapper;
import com.aiweb.service.EmailService;
import com.aiweb.service.VerificationService;
import com.aiweb.utils.SmsUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Random;


@Service
public class VerificationServiceImpl implements VerificationService {

    @Autowired
    private VerificationMapper verificationMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebClient webClient;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private EmailService emailService;

    public void sendAndsaveVerification(String userEmail, String title,String code, LocalDateTime expireTime) throws Exception {
        try{
            Verification verification=new Verification();
            verification.setUserEmail(userEmail);
            verification.setVerificationCode(passwordEncoder.encode(code));
            verification.setCreateTime(LocalDateTime.now());
            verification.setExpireTime(expireTime);
            verification.setResult(false);
            verificationMapper.insert(verification);
            emailService.sendEmail(userEmail,title,code);
        }catch (Exception e){
            throw new Exception("邮件发送失败,原因是:{}",e);
        }

    }

    public boolean checkCode(String userEmail,String inputCode)
    {
        // 检查输入参数是否为空
        if (inputCode == null || inputCode.trim().isEmpty()) {
            return false;
        }
        
        //查询最新的有效验证码记录
        QueryWrapper<Verification> wrapper=new QueryWrapper<>();
        wrapper.eq("user_email",userEmail)
                .gt("expire_time",LocalDateTime.now())
                .orderByDesc("create_time")
                .last("LIMIT 1");

        Verification lastVerification =verificationMapper.selectOne(wrapper);
        //没有验证码
        if (lastVerification==null)
        {
            return false;
        }
        //对比验证码
        boolean isMatch =passwordEncoder.matches(inputCode,lastVerification.getVerificationCode());
        //验证码对比成功，则验证位result标为true
        if(isMatch)
        {
            lastVerification.setResult(true);
            return true;
        }
        return false;
    }
}
