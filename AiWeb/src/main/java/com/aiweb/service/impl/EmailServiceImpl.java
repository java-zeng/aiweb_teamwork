package com.aiweb.service.impl;

import com.aiweb.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String fromUserEmail;

    @Autowired
    private JavaMailSender javaMailSender;

    public String emailContent(String veriCode)
    {
        String htmlTemplate = """
        <div style="text-align: center; max-width: 400px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;">
            <p style="font-size: 16px; color: #333;">欢迎你注册AIweb!</p>
            <p style="font-size: 18px; color: #000; font-weight: bold; padding: 10px 0;">
                您的验证码为: <span style="color: #4CAF50; font-size: 24px;">%s</span>
            </p>
            <p style="font-size: 14px; color: #777;">请在5分钟内完成验证。</p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 15px 0;">
            <p style="font-size: 14px; color: #999;">AIweb 团队</p>
        </div>
        """;
        String emailContent = String.format(htmlTemplate, veriCode);
        return emailContent;
    }

    @Override
    public void sendEmail(String toUserEmail,String title,String veriCode) {
        try{
            // 1. 创建 MimeMessage 对象，用于更复杂的邮件构造
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            // 2. 设置邮件内容
            helper.setFrom(fromUserEmail);
            helper.setSubject(title);
            helper.setTo(toUserEmail);

            String content = emailContent(veriCode);

            // 关键修改：将第二个参数设置为 true，发送 HTML 内容
            helper.setText(content, true); // true 明确表示内容是 HTML Text

            // 4. 发送邮件
            javaMailSender.send(message);
            log.info("邮件成功发向:{}", toUserEmail);
        } catch (Exception e) {
            log.error("邮件发送失败！", e); // 使用 log.error 记录异常更规范
        }
    }
}
