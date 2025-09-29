package com.aiweb.service.impl;

import com.aiweb.service.EmailService;
import com.aiweb.utils.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String fromUserEmail;

    @Autowired
    private JavaMailSender javaMailSender;

    public String emailContent(String veriCode)
    {
        String emailContent="验证码为:"+veriCode+",有效期为5分钟！";
        return emailContent;
    }

    @Override
    public void sendEmail(String toUserEmail,String title,String veriCode) {
        try{
            //创建邮件对象
            SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
            //设置邮件内容
            simpleMailMessage.setFrom(fromUserEmail);
            simpleMailMessage.setSubject(title);
            simpleMailMessage.setTo(toUserEmail);
            simpleMailMessage.setText(emailContent(veriCode));
            //发送邮件
            javaMailSender.send(simpleMailMessage);
            log.info("邮件成功发向:{}",toUserEmail);
        }catch (Exception e)
        {
            log.info("邮件发送失败！");
        }
    }
}
