package com.fxs.bike.security;

import com.fxs.bike.cache.CommonCacheUtil;
import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.constants.Parameters;
import com.fxs.bike.user.entity.UserElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class RestPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    private AntPathMatcher matcher = new AntPathMatcher();

    private List<String> noneSecurityList;

    private CommonCacheUtil cacheUtil;

    public RestPreAuthenticatedProcessingFilter(List<String> noneSecurityList, CommonCacheUtil cacheUtil) {
        this.noneSecurityList = noneSecurityList;
        this.cacheUtil = cacheUtil;
    }

    //preAuth得到用户信息后，会调用进入AuthenticationManager，然后授权管理器会调用1个或N个provider进行权限校验
    //如果没有通过，则进入entrypoint进行异常处理
    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        GrantedAuthority[] authorities = new GrantedAuthority[1];
        //如果是无需权限校验的url或OPTIONS，随便生成一个角色赋给他
        if(isNoneSecurity(request.getRequestURL().toString()) || "OPTIONS".equals(request.getMethod())) {
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SOME");
            authorities[0] = authority;
            return new RestAuthenticationToken(Arrays.asList(authorities));
        }
        //检查token和版本
        String token = request.getHeader(Constants.REQUEST_TOKEN_KEY);
        String version = request.getHeader(Constants.REQUEST_VERSION_KEY);
        if(version == null) {
            request.setAttribute("header-error", 400);
        }
        if(request.getAttribute("header-error") == null) {
            try {
                if(!StringUtils.isBlank(token)) {
                    UserElement ue = cacheUtil.getUserByToken(token);
                    if(ue instanceof UserElement) {
                        //检查到token说明用户已经登录且未过期，授权给BIKE_CLIENT角色 允许访问
                        GrantedAuthority authority = new SimpleGrantedAuthority("BIKE_CLIENT");
                        authorities[0] = authority;
                        RestAuthenticationToken authToken = new RestAuthenticationToken(Arrays.asList(authorities));
                        authToken.setUser(ue);
                        return authToken;
                    }
                }
                else {
                    log.warn("Get no token from request header");
                    //token不存在，告诉移动端登录
                    request.setAttribute("header-error", 401);
                }
            } catch (Exception e) {
                log.error("Fail to authenticate user", e);
            }
        }
        if(request.getAttribute("header-error") != null) {
            //请求头有错，随便给个角色，让逻辑进入到provider
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_NONE");
            authorities[0] = authority;
        }
        RestAuthenticationToken authToken = new RestAuthenticationToken(Arrays.asList(authorities));
        return authToken;
    }

    private boolean isNoneSecurity(String uri) {
        boolean result = false;
        if(this.noneSecurityList != null) {
            for(String pattern : this.noneSecurityList) {
                if(matcher.match(pattern, uri)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
        return null;
    }
}
