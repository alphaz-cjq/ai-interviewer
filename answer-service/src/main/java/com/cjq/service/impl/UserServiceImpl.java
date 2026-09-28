package com.cjq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cjq.Exceptions.BusinessException;
import com.cjq.constant.RedisKeyConstants;
import com.cjq.mapper.UserMapper;
import com.cjq.pojo.DTO.LoginRequest;
import com.cjq.pojo.DTO.RegisterRequest;
import com.cjq.pojo.PO.User;
import com.cjq.pojo.common.Result;
import com.cjq.service.UserService;
import com.cjq.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor// 生成构造函数
public class UserServiceImpl implements UserService {

     private final UserMapper userMapper;
     private final RedisTemplate<String, String> redisTemplate; //注入redis
    private final BCryptPasswordEncoder passwordEncoder;
     private final JwtUtil jwtUtil;//拦截

/*
* 用户登录
* */
    @Override
    public String login(LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        //1.查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
        );
        //2.检验用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        //3.校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        //4.生成JWT,返回给controller层
        return jwtUtil.generateJwt(Map.of("userId", user.getId()));
    }

    /*
     * 用户注册
     * */

    @Override
    public Result<Void> register(RegisterRequest request) {

        //1.从redis当中获取验证码
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.EMAIL_CODE_PREFIX, request.getEmail());//拼接key
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);//从redis获取验证码

        //2.验证验证码是否正确
        // 验证码是否为空
        if (cachedCode == null){
            throw new BusinessException("验证码已过期，请重新获取");
        }
        // 验证码是否匹配
        if (!cachedCode.equals(request.getCode())){
            throw new BusinessException("验证码错误");
        }

        //3.检查用户名是否已存在
        //3.1 查询用户
       User user= userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        //3.2 判断用户是否存在
        if (user != null) {
            throw new BusinessException("用户名已存在");
        }

        //4.检查邮箱是否已存在
        //4.1 查询用户
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail()));
        //4.2 判断用户是否存在
        if (count != null && count > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        //5.密码加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        //6.保存用户信息到数据库
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(encodedPassword);
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setRole("user");
        newUser.setStatus(1);

        userMapper.insert(newUser);

        //7.验证码用完后立即删除（防止删除）
        redisTemplate.delete(cacheKey);

        log.info("用户 {} 注册成功", request.getUsername());
        return Result.success();
    }

    /*
    * 验证码功能
    * */
    @Override
    public Result<Void> sendVerificationCode(String email) {
        //1.生成6位随机验证码
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        log.info("生成的验证码是：{}", code);

        //2.将验证码存入Redis，5分钟过期
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.EMAIL_CODE_PREFIX, email);
        redisTemplate.opsForValue().set(cacheKey,code,5, TimeUnit.MINUTES);//存入Redis

        //3.发送邮件
        log.info("【模拟发送邮件】给 {} 发送验证码：{}", email, code);
        return Result.success();
    }

    /*
    * 找回密码
    * */
    @Override
    public Result<Void> resetPassword(String email, String code, String newPassword) {
        //1.从redis获取验证码
        String cacheKey = RedisKeyConstants.buildKey(RedisKeyConstants.EMAIL_CODE_PREFIX, email);
        String cachedCode = redisTemplate.opsForValue().get(cacheKey);

        //2.验证验证码是否正确
        if (cachedCode == null){
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(code)){
            throw new BusinessException("验证码错误");
        }

        //3.检查邮箱对应的用户是否存在
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        //4.密码加密
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        //5.更新用户信息到数据库
        //找回密码是更新操作
        userMapper.updateById(user);

        //6.验证码用完后立即删除（防止被利用）
        redisTemplate.delete(cacheKey);

        log.info("用户 {} 找回密码成功", user.getUsername());
        return Result.success();
    }
}
