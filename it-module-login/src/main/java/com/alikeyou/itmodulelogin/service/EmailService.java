package com.alikeyou.itmodulelogin.service;

/**
 * 邮件服务接口
 * 用于发送邮件验证码
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否发送成功
     */
    boolean sendVerifyCodeEmail(String email, String code);

}