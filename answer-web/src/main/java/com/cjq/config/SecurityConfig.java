package com.cjq.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
* 整个安检流程的总指挥
*1. 决定哪些通道不检票（permitAll）：比如登录、注册接口（/api/auth/**），谁都能进，不用查票。
*2.决定哪些通道必须检票（authenticated）：比如上传简历、开始面试，必须出示登机牌。
*3.安装“读卡器”（addFilterBefore）：告诉机场，在常规人工检票口（UsernamePasswordAuthenticationFilter）
* 之前，先加装一个自动闸机（你的JwtAuthenticationFilter）。
*
*
*
* */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //解决方案第二种：
                // ⭐ 只拦截 REQUEST（原始请求） 和 ERROR（异常报错） dispatch，跳过 ASYNC dispatch
                //    ASYNC dispatch（如 SSE 流式推送）的安全校验在 REQUEST 阶段已经通过，不需要重复鉴权
                .securityMatcher(request ->
                        request.getDispatcherType() == DispatcherType.REQUEST ||
                        request.getDispatcherType() == DispatcherType.ERROR)
                // 1. 开启 CORS 配置（读取下面的 CorsConfigurationSource Bean）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. 禁用 CSRF（必须用 Lambda 显式调用，方法引用在 Security 6 中可能不生效）
                .csrf(csrf -> csrf.disable())
                // 3. 禁用表单登录（避免消费请求体导致 @RequestBody 为空）
                .formLogin(AbstractHttpConfigurer::disable)
                //4.设置Session为无状态（JWT不需要Session）
                .sessionManagement(session -> session.sessionCreationPolicy
                        (SessionCreationPolicy.STATELESS))
                // 5. 配置接口权限
                .authorizeHttpRequests(auth -> auth
                        // 放行 CORS 预检请求（OPTIONS 不带 Authorization 头）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行认证相关接口（登录、注册、发送验证码等）
                        .requestMatchers("/api/auth/**").permitAll()
                        // 放行 Knife4j 文档页面
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                        // 放行错误页面（避免 Security 异常后 /error 再次被拦截）
                        .requestMatchers("/error").permitAll()
                        // 其他所有接口需要认证
                        .anyRequest().authenticated()
                )
                // 6. 禁用 HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable)
                //7.添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
    * CORS 跨域配置（允许前端访问）
    * */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许前端地址（开发环境）
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("http://localhost:3000");
        // 允许携带凭证（如 Cookie / Authorization Header）
        configuration.setAllowCredentials(true);
        // 允许所有请求头
        configuration.addAllowedHeader("*");
        // 允许所有方法（GET、POST、PUT、DELETE 等）
        configuration.addAllowedMethod("*");
        // 暴露 Authorization 头给前端 JS 读取
        configuration.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;

    }
    // 注册 BCryptPasswordEncoder，供 Controller 和 Service 注入使用
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
