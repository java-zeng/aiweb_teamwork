package com.aiweb.service.impl;

import com.aiweb.entity.User;
import com.aiweb.entity.Verification;
import com.aiweb.mapper.VerificationMapper;
import com.aiweb.service.EmailService;
import com.aiweb.service.VerificationService;
import com.aiweb.utils.SmsUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Slf4j
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
        // 1. 查找该邮箱是否已存在记录
        QueryWrapper<Verification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_email", userEmail);
        Verification existingVerification = verificationMapper.selectOne(queryWrapper);

        // 2. 实现 "1分钟发送冷却" 逻辑
        if (existingVerification != null &&
                ChronoUnit.SECONDS.between(existingVerification.getCreateTime(), LocalDateTime.now()) < 60) {
            // 如果记录存在，并且距离上次创建时间不足60秒
            throw new Exception("操作过于频繁，请稍后再试");
        }

        // 3. 准备验证码数据
        String encodedCode = passwordEncoder.encode(code);
        LocalDateTime now = LocalDateTime.now();


        if (existingVerification != null) {
            // 记录已存在，执行更新操作
            existingVerification.setVerificationCode(encodedCode);
            existingVerification.setCreateTime(now);
            existingVerification.setExpireTime(expireTime);
            existingVerification.setResult(null); // 重置验证结果
            verificationMapper.updateById(existingVerification);
        } else {
            // 记录不存在，执行插入操作
            Verification newVerification = new Verification();
            newVerification.setUserEmail(userEmail);
            newVerification.setVerificationCode(encodedCode);
            newVerification.setCreateTime(now);
            newVerification.setExpireTime(expireTime);
            // result 字段默认为 null，无需显式设置
            verificationMapper.insert(newVerification);
        }

        // 4. 发送邮件
        try {
            emailService.sendEmail(userEmail, title, code);
        } catch (Exception e) {
            // 如果邮件发送失败，为了数据一致性，应该抛出异常，让事务回滚
            log.error("邮件发送失败，数据库操作将回滚", e);
            throw new Exception("邮件发送失败，请检查您的邮箱地址或稍后再试");
        }
    }

    public boolean checkCode(String userEmail, String inputCode) {
        // 检查输入参数是否为空
        if (inputCode == null || inputCode.trim().isEmpty()) {
            return false;
        }

        // 查询最新的有效验证码记录
        QueryWrapper<Verification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_email", userEmail)
                .gt("expire_time", LocalDateTime.now()) // 检查是否过期
                .isNull("result") // 确保是尚未验证过的记录
                .orderByDesc("create_time")
                .last("LIMIT 1");

        Verification lastVerification = verificationMapper.selectOne(wrapper);

        // 没有找到有效的验证码记录
        if (lastVerification == null) {
            return false;
        }

        // 对比验证码
        boolean isMatch = passwordEncoder.matches(inputCode, lastVerification.getVerificationCode());

        // 验证码对比成功，则更新 result 字段为 true
        if (isMatch) {
            lastVerification.setResult(true);
            // 关键：将更新持久化到数据库
            verificationMapper.updateById(lastVerification);
            return true;
        }

        return false;
    }
}
