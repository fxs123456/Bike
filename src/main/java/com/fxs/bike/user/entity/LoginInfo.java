package com.fxs.bike.user.entity;

import lombok.Data;

@Data
public class LoginInfo {
    //加密的手机号和验证码等
    private String data;
    //RSA加密过的AES密钥
    private String key;
}
