package com.fxs.bike.sms;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.utils.HttpUtil;
import com.fxs.bike.security.MD5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service("verCodeService")
@Slf4j
public class MiaoDiSmsSender implements SmsSender{
    @Override
    public  void sendSms(String phone, String params){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            String sig = MD5Util.getMD5(Constants.MDSMS_ACCOUNT_SID +Constants.MDSMS_AUTH_TOKEN +timestamp);
            String url = Constants.MDSMS_REST_URL;
            Map<String,String> param = new HashMap<>();
            param.put("accountSid",Constants.MDSMS_ACCOUNT_SID);
            param.put("to",phone);
            param.put("smsContent", "您的验证码是{1}，请勿泄露。");
            param.put("param",params);
            param.put("timestamp",timestamp);
            param.put("sig",sig);
            param.put("respDataType","json");
            String result = HttpUtil.post(url,param);
            JSONObject jsonObject = JSON.parseObject(result);
            if(!jsonObject.getString("respCode").equals("0000")){
                log.error("fail to send sms to "+phone+":"+params+":"+result);
            }
        } catch (Exception e) {
            log.error("fail to send sms to "+phone+":"+params);
        }
    }
}
