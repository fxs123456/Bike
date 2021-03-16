package com.fxs.bike.common.exception;

import org.springframework.security.core.AuthenticationException;

public class BadCredentialException extends AuthenticationException {
    public BadCredentialException(String msg) {
        super(msg);
    }
}
