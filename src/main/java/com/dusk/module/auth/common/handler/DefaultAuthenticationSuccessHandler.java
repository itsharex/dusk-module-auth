package com.dusk.module.auth.common.handler;

import com.dusk.common.core.model.UserContext;
import com.dusk.common.core.response.BaseApiResult;
import com.dusk.module.auth.common.manage.TokenAuthManager;
import com.dusk.module.auth.service.ICaptchaService;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author kefuming
 * @date 2020-04-23 8:37
 */
@Component
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Resource
    private ObjectMapper mapper;
    @Resource
    private TokenAuthManager tokenAuthManager;
    @Resource
    private ICaptchaService captchaService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        UserContext userContext = (UserContext) authentication.getPrincipal();
        String accessToken = tokenAuthManager.generateToken(userContext);
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        mapper.writeValue(httpServletResponse.getWriter(), BaseApiResult.success(accessToken, "登录成功"));
        clearAuthenticationAttributes(httpServletRequest);

        //清除错误缓存
        captchaService.clearBuffer(httpServletRequest);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }
}
