package com.telechat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telechat.pojo.result.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * 当未登录或者token失效访问接口时，自定义的返回结果
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 设置响应头
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        // 构建返回结果 (假设Result类是你项目中的统一返回对象)
        // 这里可以根据需求细化，比如判断 authException 的类型来返回"Token已过期"或"Token无效"
        Result<String> result = Result.error(401, "用户未登录或登录信息已过期，请重新登录");
        
        // 写入响应
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}