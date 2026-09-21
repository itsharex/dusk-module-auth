package com.dusk.module.auth.common.handler;

import com.dusk.common.core.exception.UserLoginException;
import com.dusk.common.core.jwt.exception.JwtExpiredTokenException;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.module.auth.service.ICaptchaService;
import com.dusk.module.auth.service.impl.UserServiceImpl;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kefuming
 * @date 2020-04-23 9:02
 */
@Component
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Resource
    private ObjectMapper mapper;
    @Resource
    private ICaptchaService captchaService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());

        if (e instanceof BadCredentialsException || e instanceof UsernameNotFoundException) {
            boolean needCaptcha = captchaService.checkAndWriteError(httpServletRequest);
            httpServletResponse.setStatus(HttpStatus.OK.value());
            BaseApiResult result = BaseApiResult.errorServer(e.getMessage());
            if (needCaptcha) {
                result.setCode(UserServiceImpl.CODE_USER_VALID);
                result.setSuccess(true);
            }
            mapper.writeValue(httpServletResponse.getWriter(), result);
        } else if (e instanceof JwtExpiredTokenException) {
            mapper.writeValue(httpServletResponse.getWriter(), BaseApiResult.errorServer("登陆已过期，请重新登陆"));
        }
        if (e instanceof UserLoginException) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            BaseApiResult result = new BaseApiResult(((UserLoginException) e).getCode(), e.getMessage(), null, true);
            mapper.writeValue(httpServletResponse.getWriter(), result);
        }
        mapper.writeValue(httpServletResponse.getWriter(), BaseApiResult.errorServer("无效访问：" + e.getMessage()));
    }
}
