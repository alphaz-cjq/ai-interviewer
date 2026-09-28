package com.cjq.controller;

import com.cjq.pojo.DTO.LoginRequest;

import com.cjq.pojo.DTO.RegisterRequest;
import com.cjq.pojo.common.Result;
import com.cjq.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "认证管理", description = "登录、注册、找回密码等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor//生成构造函数
public class UserController {
    //不推荐使用Autowired，推荐使用构造函数注入
    //只依赖 service层
    private final UserService userService;//将 UserService 注入

    /*
     * 登录功能
     * */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户密码登录，返回JWT Token")
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        log.info("用户登录请求：username={}", loginRequest.getUsername());
        //2.直接调用service层获取业务结果，异常由全局处理器接管
        //生成JWT
        String token = userService.login(loginRequest);
        //3.包装成统一格式返回结果
        return Result.success(token);
    }

    /*
     * 注册功能
     *
     * */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "需要先调用发送验证码接口获取验证")
    public Result<Void> register(@RequestBody RegisterRequest registerRequest) {
        //1.日志记录
        log.info("用户注册请求：{}", registerRequest);
        //2.调用service层注册用户
        userService.register(registerRequest);
        //3.返回结果
        return Result.success("注册成功", null);
    }


    /*
     * 发送邮件验证码功能
     *
     * */
    @PostMapping("/send-code")
    @Operation(summary = "发送邮件验证码", description = "注册前调用，验证码有效期5分钟")
    public Result<Void> sendCode(@RequestParam("email") String email) {
        log.info("发送验证码请求：{}", email);
        userService.sendVerificationCode(email);
        return Result.success("验证码已发送", null);
    }

    /*
    * 找回密码
    *
    * */
    @PostMapping("/reset-password")
    @Operation(summary = "找回密码", description = "通过邮箱验证码重置密码")
    public Result<Void> resetPassword(@RequestParam("email") String email,
                                      @RequestParam("code") String code,
                                      @RequestParam("newPassword") String newPassword) {
        log.info("找回密码请求：email={}", email);
        userService.resetPassword(email, code, newPassword);
        return Result.success("密码已重置", null);
    }
}

