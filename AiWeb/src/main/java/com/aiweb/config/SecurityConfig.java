package com.aiweb.config;


import com.aiweb.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfig {
    @Autowired
    @Lazy
    private JwtAuthFilter jwtAuthFilter;


    @Autowired
    @Lazy
    private UserService userService;

    /**
     * Spring Security 配置,返回AuthenticationManager对象，我们可以调用其authenticate()方法来进行验证
     * @param config
     * @return
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 返回密码加密器的实例，是加密和对比的工具
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        // DaoAuthenticationProvider 是 Spring Security 提供的用于数据库认证的 Provider
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // 告诉 Provider 从哪里获取用户信息 (我们自己写的 UserService)
        authProvider.setUserDetailsService(userService);
        // 告诉 Provider 使用哪种密码加密器
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,AuthenticationProvider authenticationProvider) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // 关闭 CSRF 防护

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // 允许 /api/auth/ 下的接口被匿名访问
                        .requestMatchers("/api/auth/register").permitAll() // 明确允许注册接口
                        .requestMatchers("/api/auth/sendCode").permitAll() // 明确允许发送验证码接口
                        .requestMatchers("/api/auth/login").permitAll() // 明确允许登录接口
                        .requestMatchers("/api/v1/collections/**").permitAll()
                        .requestMatchers("/api/fastgpt/health").permitAll() // 允许FastGPT健康检查
                        .requestMatchers("/api/user-role-kb/**").authenticated() // 角色知识库API需要认证
                        .requestMatchers("/api/fastgpt/**").authenticated() // FastGPT API需要认证
                        .anyRequest().authenticated() // 其他所有请求都需要认证
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 设置为无状态会话
                )

                .authenticationProvider(authenticationProvider) // 设置我们自定义的 AuthenticationProvider

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // 添加 JWT 过滤器

                .build();
    }
}
