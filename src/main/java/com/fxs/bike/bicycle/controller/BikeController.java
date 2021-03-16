package com.fxs.bike.bicycle.controller;

import com.fxs.bike.bicycle.entity.Bicycle;
import com.fxs.bike.bicycle.entity.BikeLocation;
import com.fxs.bike.bicycle.entity.Point;
import com.fxs.bike.bicycle.service.BikeGeoService;
import com.fxs.bike.bicycle.service.BikeService;
import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.common.resp.ApiResult;
import com.fxs.bike.common.rest.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("bike")
@Slf4j
public class BikeController extends BaseController {

    @Autowired
    private BikeGeoService bikeGeoService;

    @Autowired
    private BikeService bikeService;

    @RequestMapping("/generateBike")
    public ApiResult generateBike() {
        ApiResult resp = new ApiResult();
        try {
            bikeService.generateBike();
            resp.setMessage("创建单车成功");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to update bike info", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }

    @RequestMapping("/findAroundBike")
    public ApiResult<List<BikeLocation>> findAroundBike(@RequestBody Point point ){

        ApiResult<List<BikeLocation>> resp = new ApiResult<>();
        try {
            List<BikeLocation> bikeList = bikeGeoService.geoNear("bike_position",null,point,10,50);
            resp.setMessage("查询附近单车成功");
            resp.setData(bikeList);
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to find around bike info", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }

    @RequestMapping("/unLockBike")
    public ApiResult unLockBike(@RequestBody Bicycle bike){

        ApiResult resp = new ApiResult();
        try {
            //先获取ue，在获取channelID
            bikeService.unLockBike(getCurrentUser(), bike.getNumber());
            resp.setMessage("等待单车解锁");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to unlock bike ", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }

    @RequestMapping("/lockBike")
    public ApiResult lockBike(@RequestBody BikeLocation bikeLocation) {
        ApiResult resp = new ApiResult();
        try {
            //锁车时必须把bikeNO和车坐标传给服务器
            //锁车操作是自行车去调用的，无法获得当前用户的信息
            //所以这个接口不能被springsecurity拦截，因为自行车是没有token的
            //但是也可以给自行车生成一个固定的token
            bikeService.lockBike(bikeLocation);
            resp.setMessage("锁车成功");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to lock bike", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }
        return resp;
    }

    @RequestMapping("/reportLocation")
    public ApiResult reportLocation(@RequestBody BikeLocation bikeLocation){

        ApiResult resp = new ApiResult();
        try {
            bikeService.reportLocation(bikeLocation);
            resp.setMessage("上报坐标成功");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to report location", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }

}
