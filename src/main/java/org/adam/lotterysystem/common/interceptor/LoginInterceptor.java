package org.adam.lotterysystem.common.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.adam.lotterysystem.common.utils.JWTUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 预处理，在请求处理之前进行拦截，可以用于权限验证、日志记录等操作。
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头
        String token = request.getHeader("user_token");
        log.info("获取到请求头中的token: {}", token);
        log.info("请求的URL: {}", request.getRequestURI());
        // 令牌解析
        Claims claims = JWTUtil.parseJWT(token);
        if (null == claims) {
            log.error("令牌解析失败，拒绝访问");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        log.info("令牌解析成功，用户ID: {}", claims.getSubject());
        return true;
    }
}
