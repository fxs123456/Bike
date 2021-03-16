package com.fxs.bike.security;

import com.fxs.bike.common.exception.BadCredentialException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class RestAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if(authentication instanceof PreAuthenticatedAuthenticationToken) {
            PreAuthenticatedAuthenticationToken preAuth = (PreAuthenticatedAuthenticationToken) authentication;
            RestAuthenticationToken sysAuth = (RestAuthenticationToken) preAuth.getPrincipal();
            if(sysAuth.getAuthorities() != null && sysAuth.getAuthorities().size() > 0) {
                GrantedAuthority auth = sysAuth.getAuthorities().iterator().next();
                if("BIKE_CLIENT".equals(auth.getAuthority())) return sysAuth;
                else if("ROLE_SOME".equals(auth.getAuthority())) return sysAuth;

            }

        }
        else if (authentication instanceof RestAuthenticationToken) {
            RestAuthenticationToken sysAuth = (RestAuthenticationToken) authentication;
            if (sysAuth.getAuthorities() != null && sysAuth.getAuthorities().size() > 0) {
                GrantedAuthority auth = sysAuth.getAuthorities().iterator().next();
                if ("BIKE_CLIENT".equals(auth.getAuthority())) {
                    return sysAuth;
                }else if ("ROLE_SOME".equals(auth.getAuthority())) {
                    return sysAuth;
                }
            }
        }
        throw new BadCredentialException("unknown.error");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        //返回true才会执行授权方法,即有对应的provider进行校验
        return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication) ||
                RestAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
