package com.fxs.bike.security;

import com.alibaba.fastjson.JSON;
import com.fxs.bike.common.resp.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        ApiResult result = new ApiResult();
        //检查头部错误
        if(request.getAttribute("header-error") != null) {
            if ("400".equals(request.getAttribute("header-error") + "")) {
                result.setCode(408);
                result.setMessage("请升级app至最新版本");
            }
            else {
                result.setCode(401);
                result.setMessage("请您登录");
            }
        }
        try {
            //设置跨域请求 请求结果json刷到响应里,前后端分离的时候都要设置
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, HEADER");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "X-Requested-With, user-token, Content-Type, Accept, version, type, platform");
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.getWriter().write(JSON.toJSONString(result));
            response.flushBuffer();
        } catch (Exception er) {
            log.error("Fail to send 401 response {}", er.getMessage());
        }
    }
}
