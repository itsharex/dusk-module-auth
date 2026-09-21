package com.dusk.module.auth.common.filter;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.dusk.common.core.auditlog.SysLogEvent;
import com.dusk.common.core.dto.AuditLogDto;
import com.dusk.module.auth.common.model.LoginRequest;
import com.dusk.module.auth.enums.LoginLogType;
import com.dusk.module.auth.listener.LogInOutEvent;
import com.dusk.module.auth.service.ICaptchaService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author kefuming
 * @date 2020-05-20 17:01
 */
public class JWTAuthenticationFilter extends AbstractAuthenticationProcessingFilter {


    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;

    private final ObjectMapper objectMapper;

    private final ICaptchaService captchaService;

    private final ApplicationEventPublisher publisher;


    public JWTAuthenticationFilter(String defaultProcessUrl, AuthenticationSuccessHandler successHandler,
                                   AuthenticationFailureHandler failureHandler, ObjectMapper mapper, ICaptchaService captchaService, ApplicationEventPublisher publisher) {
        super(defaultProcessUrl);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.objectMapper = mapper;
        this.captchaService = captchaService;
        this.publisher = publisher;
    }


    //由于 /login 这种 无法被全局aop拦截，这里手动暴露下日志
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        UsernamePasswordAuthenticationToken token;
        AuditLogDto auditLog = new AuditLogDto();
        StopWatch sw = new StopWatch();
        sw.start();
        auditLog.setExecutionTime(LocalDateTime.now(ZoneId.systemDefault()));
        auditLog.setBrowserInfo(request.getHeader("User-Agent"));
        auditLog.setClientIpAddress(JakartaServletUtil.getClientIP(request));
        auditLog.setMethodName("login");
        auditLog.setServiceName("login");

        LogInOutEvent logInOutEvent = new LogInOutEvent(LoginLogType.LOGIN_IN, LocalDateTime.now(ZoneId.systemDefault()), false);
        logInOutEvent.setIp(JakartaServletUtil.getClientIP(request));
        logInOutEvent.setBrowserInfo(request.getHeader("User-Agent"));
        try {
            try {
                LoginRequest loginRequest = objectMapper.readValue(request.getReader(), LoginRequest.class);
                LoginRequest copy = new LoginRequest();
                BeanUtil.copyProperties(loginRequest, copy);
                copy.setPassword("***");
                auditLog.setParameters(objectMapper.writeValueAsString(copy));
                logInOutEvent.setUserName(loginRequest.getUsername());
                boolean verifyCaptcha = captchaService.verifyCaptcha(loginRequest, request);
                if (!verifyCaptcha) {
                    throw new BadCredentialsException("验证码错误");
                }
                if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
                    throw new BadCredentialsException("帐户名或密码不能为空");
                }

                token = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
            } catch (MismatchedInputException ex) {
                throw new BadCredentialsException("帐户名或密码不能为空");
            }
            Authentication authentication = this.getAuthenticationManager().authenticate(token);
            logInOutEvent.setSuccess(authentication.isAuthenticated());
            return authentication;
        } catch (Exception e) {
            auditLog.setException(ExceptionUtil.stacktraceToOneLineString(e));
            logInOutEvent.setMsg(StrUtil.subPre(e.getMessage(), 1000));
            throw e;
        } finally {
            sw.stop();
            auditLog.setExecutionDuration((int) sw.getLastTaskTimeMillis());
            publisher.publishEvent(new SysLogEvent(auditLog));
            publisher.publishEvent(logInOutEvent);
        }

    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    // 这是验证失败时候调用的方法
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
