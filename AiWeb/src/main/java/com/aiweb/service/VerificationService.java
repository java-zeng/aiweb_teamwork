package com.aiweb.service;
import java.time.LocalDateTime;

public interface VerificationService {
    //将要验证的用户信息存入对应的实例对象中
    public void sendAndsaveVerification(String userEmail,String title, String code, LocalDateTime expireTime) throws Exception;

    //将用户输入的验证和生成的验证码进行对比
    public boolean checkCode(String phoneNumber,String inputCode);



}
