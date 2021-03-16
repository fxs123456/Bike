package com.fxs.bike.user.controller;

import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.common.resp.ApiResult;
import com.fxs.bike.common.rest.BaseController;
import com.fxs.bike.user.entity.LoginInfo;
import com.fxs.bike.user.entity.User;
import com.fxs.bike.user.entity.UserElement;
import com.fxs.bike.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("user")
@Slf4j
public class UserController extends BaseController {
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    //验证用户加密的信息进行解密，生成token返回给用户，用户以后每次请求带着token
    public ApiResult<String> login(@RequestBody LoginInfo loginInfo){
        ApiResult<String> resp = new ApiResult<>();
        String data = loginInfo.getData();
        String key = loginInfo.getKey();
        try{
            if(StringUtils.isBlank(data) || StringUtils.isBlank(key)) {
                throw new BikeException("参数校验失败");
            }
            String token = userService.login(data, key);
            resp.setData(token);
        } catch (BikeException e) {
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage(e.getMessage());
        }  catch (Exception e) {
            log.error("Fail to login", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }
        return resp;
    }

    @RequestMapping("/modifyNickName")
    //只需要去校验token是否存在，不需要去校验url权限（没有分不同的用户和不同的角色）
    //预授权：在别的系统或代码中进行过授权，不需要springSecurity进行权限控制（只需要校验token）
    public ApiResult modifyNickName(@RequestBody User user) {
        ApiResult resp = new ApiResult();
        try {
            //问题：想要修改的用户id不能从前端获取，（不安全），必须从后台获取，可以
            //通过token来获取用户信息
            UserElement ue = getCurrentUser();
            user.setId(ue.getUserId());
            userService.modifyNickName(user);
            resp.setMessage("更新成功");
        } catch (BikeException e) {
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to update", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }
        return resp;
    }

    @RequestMapping("/sendVercode")
    //短信接口的安全性，根据ip来判断，超过十次就不发送，同一个手机号，一段时间内不能超过多少次
    public ApiResult sendVercode(@RequestBody User user, HttpServletRequest request) {
        ApiResult resp = new ApiResult();
        try {
            userService.sendVercode(user.getMobile(), getIpFromRequest(request));
        } catch (BikeException e) {
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to send sms vercode", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }
        return resp;
    }

    @RequestMapping(value = "uploadHeadImg", method = RequestMethod.POST)
    public ApiResult<String> uploadHeadImg(HttpServletRequest request, @RequestParam(required = false)MultipartFile file) {
        ApiResult<String> resp = new ApiResult<>();
        try {
            UserElement ue = getCurrentUser();
            String url = userService.uploadHeadImg(file,ue.getUserId());
            resp.setMessage("上传成功");
            resp.setData(url);
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to update user info", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }
        return resp;
    }
}
