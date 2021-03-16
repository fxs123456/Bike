package com.fxs.bike.user.service;

import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    String login(String data, String key) throws BikeException;

    void modifyNickName(User user) throws BikeException;

    void sendVercode(String mobile, String ip) throws BikeException;

    String uploadHeadImg(MultipartFile file, Long userId) throws BikeException;
}
