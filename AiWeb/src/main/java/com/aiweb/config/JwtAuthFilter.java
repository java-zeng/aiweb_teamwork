package com.aiweb.config;

import com.aiweb.dto.UserDto;
import com.aiweb.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT过滤器，用于验证每个请求中的jwt令牌
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {//继承的目的是让令牌校验在请求的周期内只被执行一次
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        //1.从请求头获取Authorization字段
        final String authHeader=request.getHeader("Authorization");
        String username=null;
        String jwt=null;
        //2.检查header是否存在，是否有"Bearer "开头
        if(authHeader==null || !authHeader.startsWith("Bearer "))
        {
            //不符合条件直接放行，让后续的过滤器继续判断
            filterChain.doFilter(request,response);
            return;
        }
        //3. 提取 JWT 令牌 (去掉 "Bearer " 前缀)
        int BearerLength="Bearer ".length();
        jwt=authHeader.substring(BearerLength);
        try{
            //4.从jwt令牌中解析出用户名
            username=jwtUtil.extractUsername(jwt);
        }catch (Exception e){
            // 如果解析出错（例如令牌过期、格式错误），直接放行
            // 后续的 Spring Security 过滤器会因为 SecurityContext 中没有认证信息而拒绝该请求
            logger.warn("JWT token parsing error: " + e.getMessage());
        }
        // 5. 如果成功解析出用户名，并且当前 SecurityContext 中还没有认证信息
        if(username!=null&& SecurityContextHolder.getContext().getAuthentication()==null)
        {
            //6.根据用户名，从数据库加载信息，生成UserDetails格式
            UserDetails userDetails=this.userDetailsService.loadUserByUsername(username);
            //7.验证令牌是否有效
            if(jwtUtil.validateToken(jwt,userDetails))
            {
                //8.如果令牌有效，创建一个已认证的Authentication对象
                UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,//因为是令牌验证，所以不需要密码
                        userDetails.getAuthorities()//用户的权限信息
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //9.将该Authentication对象设置到SecurityContext中
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        // 10. 无论验证结果如何，都放行请求，让过滤器链继续执行
        filterChain.doFilter(request, response);
    }
}
