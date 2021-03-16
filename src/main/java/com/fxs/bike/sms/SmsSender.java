package com.fxs.bike.sms;

public interface SmsSender {

    void sendSms(String phone, String params);
}
