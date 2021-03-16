package com.fxs.bike.user.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fxs.bike.cache.CommonCacheUtil;
import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.common.utils.QiniuFileUploadUtil;
import com.fxs.bike.common.utils.RandomNumberCode;
import com.fxs.bike.jms.SmsProcessor;
import com.fxs.bike.security.AESUtil;
import com.fxs.bike.security.Base64Util;
import com.fxs.bike.security.MD5Util;
import com.fxs.bike.security.RSAUtil;
import com.fxs.bike.user.dao.UserMapper;
import com.fxs.bike.user.entity.User;
import com.fxs.bike.user.entity.UserElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.jms.Destination;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService{
    private static final String SMS_QUEUE = "sms.queue";
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommonCacheUtil cacheUtil;

    private static final String VERIFYCODE_PREFIX = "verify.code.";

    @Autowired
    private SmsProcessor smsProcessor;

    @Override
    public String login(String data, String key) throws BikeException {
        String token = null;
        String decryptData = null;
        try {
            byte[] aesKey = RSAUtil.decryptByPrivateKey(Base64Util.decode(key));
            decryptData = AESUtil.decrypt(data, new String(aesKey, "UTF-8"));
            if(decryptData == null) {
                throw new Exception();
            }
            JSONObject jsonObject = JSON.parseObject(decryptData);
            String mobile = jsonObject.getString("mobile");
            String code = jsonObject.getString("code");
            String platform = jsonObject.getString("platform");
            String channelId = jsonObject.getString("channelId");
            if(StringUtils.isBlank(mobile) || StringUtils.isBlank(code)
                    || StringUtils.isBlank(platform) || StringUtils.isBlank(channelId)) {
                throw new Exception();
            }
            //去redis取验证码，比较手机号码和验证码是否匹配
            String verCode = cacheUtil.getCacheValue(mobile);
            User user;
            if(code.equals(verCode)) {
                //匹配
                //判断用户是否在数据库中存在，， 不存在帮他注册，插入数据库
                user = userMapper.selectByMobile(mobile);
                if(user == null) {
                    user = new User();
                    user.setMobile(mobile);
                    user.setNickname(mobile);
                    userMapper.insertSelective(user);
                }
            }
            else {
                throw new BikeException("手机号验证码不匹配");
            }
            //存在生成token，存redis
            try {
                token = generateToken(user);
            } catch (Exception e) {
                throw new BikeException("生成token失败");
            }
            UserElement ue = new UserElement();
            ue.setMobile(mobile);
            ue.setUserId(user.getId());
            ue.setToken(token);
            ue.setPlatform(platform);
            ue.setPushChannelId(channelId);
            cacheUtil.putTokenWhenLogin(ue);
        } catch (Exception e) {
            log.error("Fail to decrypt data", e);
            throw new BikeException("数据解析错误");
        }
        return token;
    }

    @Override
    public void modifyNickName(User user) throws BikeException {
        userMapper.updateByPrimaryKeySelective(user);
    }

    @Override
    public void sendVercode(String mobile, String ip) throws BikeException {
        String verCode = RandomNumberCode.verCode();
        int result = cacheUtil.cacheForVercode(VERIFYCODE_PREFIX + mobile, verCode, "reg", 60, ip);
        if (result == 1) {
            log.info("当前验证码未过期，请稍后重试");
            throw new BikeException("当前验证码未过期，请稍后重试");
        } else if (result == 2) {
            log.info("超过当日验证码次数上线");
            throw new BikeException("超过当日验证码次数上限");
        } else if (result == 3) {
            log.info("超过当日验证码次数上限 {}", ip);
            throw new BikeException(ip + "超过当日验证码次数上限");
        }
        log.info("Sending verify code {} for phone {}", verCode, mobile);
        //校验通过，发送短信
        //发短信的结果与业务逻辑无关，即发送短信成功与否不应该影响后续逻辑，用户只需重试一遍---使用MQ解耦出去
        //发消息到队列
        Destination destination = new ActiveMQQueue(SMS_QUEUE);
        Map<String,String> smsParam = new HashMap<>();
        smsParam.put("mobile",mobile);
        smsParam.put("vercode",verCode);
        String message = JSON.toJSONString(smsParam);
        smsProcessor.sendSmsToQueue(destination, message);
    }

    @Override
    public String uploadHeadImg(MultipartFile file, Long userId) throws BikeException {
        try {
            //获取user 得到原来的头像地址
            User user = userMapper.selectByPrimaryKey(userId);
            // 调用七牛
            String imgUrlName = QiniuFileUploadUtil.uploadHeadImg(file);
            user.setHeadImg(imgUrlName);
            //更新用户头像URL
            userMapper.updateByPrimaryKeySelective(user);
            return Constants.QINIU_HEAD_IMG_BUCKET_URL+"/"+imgUrlName;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new BikeException("头像上传失败");
        }
    }

    private String generateToken(User user) {
        String source = user.getId() + ":" + user.getMobile() + ":" + System.currentTimeMillis();
        return MD5Util.getMD5(source);
    }
}
