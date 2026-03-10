package com.alikeyou.itmodulelogin.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 验证码服务
 * 用于生成和验证验证码
 */
@Service
public class VerifyCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerifyCodeService.class);

    // 存储验证码，key为邮箱，value为验证码
    private final Map<String, String> verifyCodes = new HashMap<>();

    @Autowired
    private EmailService emailService;

    /**
     * 生成验证码
     * @return 6位数字验证码
     */
    public String generateVerifyCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送验证码
     * @param email 邮箱地址
     * @return 是否发送成功
     */
    public boolean sendVerifyCode(String email) {
        logger.info("开始处理发送验证码请求，邮箱：{}", email);
        
        // 生成验证码
        String code = generateVerifyCode();
        logger.info("生成验证码：{}", code);
        
        // 存储验证码
        verifyCodes.put(email, code);
        logger.info("验证码已存储，邮箱：{}", email);
        
        // 发送邮件
        logger.info("准备发送验证码邮件");
        boolean result = emailService.sendVerifyCodeEmail(email, code);
        logger.info("发送验证码到邮箱：{}，结果：{}", email, result);
        
        return result;
    }

    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = verifyCodes.get(email);
        boolean result = code != null && code.equals(storedCode);
        // 验证后删除验证码
        if (result) {
            verifyCodes.remove(email);
        }
        logger.info("验证验证码，邮箱：{}，结果：{}", email, result);
        return result;
    }

}