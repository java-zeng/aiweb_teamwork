package com.aiweb.service.impl;

import com.aiweb.entity.Verification;
import com.aiweb.mapper.VerificationMapper;
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

    @Value("${smsbao.apiurl}")
    private String apiUrl;
    @Value("${smsbao.username}")
    private String username;
    @Value("${smsbao.password}")
    private String password;
    @Value("${smsbao.contentSign}")
    private String contentSign;


    public void sendVerification(String phoneNumber)
    {
        //设置验证码过期时间5分钟
        LocalDateTime expireTime=LocalDateTime.now().plusMinutes(5);
        //生成六位验证码
        String randomCode=getCode();
        //生成短信内容格式
        String messageContent ="[" +contentSign +"]"+",您的验证码为:"+randomCode+",短信有效期为5分钟。";
        try{
            String md5password=smsUtil.md5(password);
            String urlString=smsUtil.encodeUrlString(apiUrl);
            //构建完整的Api请求地址
            String requestUrl=String.format(
                    "%s?u=%s&p=%s&m=%s&c=%s",
                    urlString,
                    username,
                    md5password,
                    phoneNumber,
                    contentSign
            );
            //发送Get请求，获取响应
            String response=webClient
                    .get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if(!response.equals("0"))
            {
                String errorMessage=smsUtil.getErrorMessage(response);
                throw new RuntimeException("短信发送失败,"+errorMessage+",错误码:"+response);
            }
            saveVerification(phoneNumber,randomCode,expireTime);
        }catch (Exception e)
        {
            throw new RuntimeException("短信发送失败"+"错误原因:"+e);
        }
    }

    public void saveVerification(String phoneNumber, String code, LocalDateTime expireTime)
    {
        Verification verification=new Verification();
        verification.setPhoneNumber(phoneNumber);
        verification.setVerificationCode(passwordEncoder.encode(code));
        verification.setCreateTime(LocalDateTime.now());
        verification.setExpireTime(expireTime);
        verification.setResult(false);
        verificationMapper.insert(verification);
    }

    /**
     * 生成6位随机数验证码
     * @return
     */
    public String getCode(){
        Random random=new Random();
        String randomCode = String.valueOf(random.nextInt(90000) + 10000);
        return randomCode;
    }

    public boolean checkCode(String phoneNumber,String inputCode)
    {
        //查询最新的有效验证码记录
        QueryWrapper<Verification> wrapper=new QueryWrapper<>();
        wrapper.eq("phone_number",phoneNumber)
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
