package com.cjq.service;


import com.cjq.pojo.DTO.LoginRequest;
import com.cjq.pojo.DTO.RegisterRequest;
import com.cjq.pojo.common.Result;

public interface UserService {

    /**
     * 用户登录
     * @return JWT token 字符串
     */
    String login(LoginRequest request);

    /**
     * 用户注册
     */
    Result<Void> register(RegisterRequest request);

    /**
     * 发送邮箱验证码
     */
    Result<Void> sendVerificationCode(String email);

    /**
     * 找回密码
     */
    Result<Void> resetPassword(String email, String code, String newPassword);
}
