package com.alikeyou.itmodulelogin.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.alikeyou.itmodulelogin.service.EmailService;

/**
 * 邮件服务实现类
 * 用于发送邮件验证码
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender javaMailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否发送成功
     */
    @Override
    public boolean sendVerifyCodeEmail(String email, String code) {
        logger.info("开始发送验证码邮件到：{}", email);
        
        // 如果JavaMailSender为null，模拟发送成功
        if (javaMailSender == null) {
            logger.warn("JavaMailSender未配置，模拟发送验证码到邮箱：{}，验证码：{}", email, code);
            logger.info("邮件发送模拟成功");
            return true;
        }
        
        try {
            logger.info("JavaMailSender已配置，准备发送邮件");
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 发件人邮箱，使用配置文件中的用户名
            message.setTo(email); // 收件人邮箱
            message.setSubject("注册验证码"); // 邮件主题
            message.setText("您的注册验证码是：" + code + "，有效期为5分钟。"); // 邮件内容

            logger.info("邮件内容准备完成，开始发送");
            javaMailSender.send(message);
            logger.info("验证码邮件发送成功：{}", email);
            return true;
        } catch (Exception e) {
            logger.error("验证码邮件发送失败：{}", email, e);
            logger.error("错误详情：{}", e.getMessage());
            return false;
        }
    }

}