package com.aiweb.service;

import java.time.LocalDateTime;

public interface VerificationService {
    //将要验证的用户信息存入对应的实例对象中
    public void saveVerification(String phoneNumber, String code, LocalDateTime expireTime);
    //获取6位随机验证码
    public String getCode();
    //将用户输入的验证和生成的验证码进行对比
    public boolean checkCode(String phoneNumber,String inputCode);

    public void sendVerification(String phoneNumber);


}
