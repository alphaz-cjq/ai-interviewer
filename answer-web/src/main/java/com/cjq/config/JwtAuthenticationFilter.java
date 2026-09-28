package com.cjq.config;

import com.cjq.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/*
*
* 1.拦截每个请求，扒开请求的帽子（Header），找 Authorization: Bearer xxxx。
*2.如果没找到帽子，直接放行（让后面的规矩去判断是否拦截）。
*3.如果找到了帽子，撕下纸条（提取Token），大喊一声：“地勤！帮我查一下这张票是真的吗？”（调用JwtUtil.parseJwt()）。
*4.如果票是真的，掏出一张临时通行证（UsernamePasswordAuthenticationToken），
* 塞进乘客口袋里（SecurityContextHolder），并告诉后面的安保：“这人我查过了，放行！”
* */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader("Authorization");

        log.info("🔍 JWT过滤器拦截: {} {} | Authorization头: {}", method, path,
                authHeader == null ? "【缺失】" : authHeader.substring(0, Math.min(20, authHeader.length())) + "...");

        // 无 token → 放行，让 SecurityConfig 决定是否拦截
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ 请求 {} {} 没有携带有效的 Bearer Token，将以匿名身份放行", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        //有token → 解析
        String token = authHeader.substring(7);
        /*
         AuthorizationFilter---------最终法官

        * AuthorizationFilter一般会到SecurityContextHolder查三个问题
        * 1.你（Authentication）到底存不存在？
        * SecurityContextHolder.getContext().getAuthentication() == null 吗？
        * 2.你是不是已经完成了认证（isAuthenticated()）？
        * authentication.isAuthenticated() 是 true 还是 false？
        * 3.你的权限（GrantedAuthority）符合我的规则吗？
        * 法官拿出 SecurityConfig 中你写的规则（比如 .anyRequest().authenticated() 或 .hasRole("ADMIN")），
        * 和 authentication.getAuthorities() 列表做对比。
        *
        * 总结：它只认一句话：SecurityContextHolder 里面必须有 Authentication 对象，
        * 并且这个对象的 isAuthenticated() 必须为 true。
只要满足这两个条件，无论你是 userId=1
         * */
        List<GrantedAuthority>authorities=Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );//给GrantedAuthority一个权限
        try {
            Claims claims = jwtUtil.parseJwt(token);//调用 JwtUtil 解析出 userId
            Long userId = claims.get("userId", Long.class);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);//将userId塞进SecurityContextHolder
            //AuthorizationFilter 查看有userId,且 authenticated=true==>通过
            log.debug("JWT认证成功，userId: {}", userId);
            log.info("🔥 JwtAuthenticationFilter 执行了，userId: {}", userId);
        } catch (Exception e) {
            // ⭐ 无效/过期 token → 返回 401，不继续放行
            log.warn("JWT解析失败，返回401: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"令牌无效或已过期\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || path.startsWith("/doc.html") || path.startsWith("/webjars/");
    }
}
