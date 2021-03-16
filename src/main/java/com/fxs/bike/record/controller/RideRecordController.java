package com.fxs.bike.record.controller;

import com.fxs.bike.bicycle.service.BikeGeoService;
import com.fxs.bike.common.constants.Constants;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.common.resp.ApiResult;
import com.fxs.bike.common.rest.BaseController;
import com.fxs.bike.record.entity.RideContrail;
import com.fxs.bike.record.entity.RideRecord;
import com.fxs.bike.record.service.RideRecordService;
import com.fxs.bike.user.entity.UserElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("rideRecord")
@RestController
@Slf4j
public class RideRecordController extends BaseController {

    @Autowired
    private BikeGeoService bikeGeoService;

    @Autowired
    private RideRecordService rideRecordService;

    @RequestMapping("/list/{id}")
    public ApiResult<List<RideRecord>> listRideRecord(@PathVariable("id") Long lastId){
        //骑行历史分页 每次传递用户界面最后一条数据的ID 下拉时候调用 每次十条数据
        //传每次页面的最后一个id，服务端加载下10条记录
        ApiResult<List<RideRecord>> resp = new ApiResult<>();
        try {
            UserElement ue = getCurrentUser();
            List<RideRecord> list = rideRecordService.listRideRecord(ue.getUserId(),lastId);
            resp.setData(list);
            resp.setMessage("查询成功");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to query ride record ", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }

    @RequestMapping("/contrail/{recordNo}")
    public ApiResult<RideContrail> rideContrail(@PathVariable("recordNo") String recordNo){

        ApiResult<RideContrail> resp = new ApiResult<>();
        try {
            UserElement ue = getCurrentUser();
            RideContrail contrail = bikeGeoService.rideContrail("ride_contrail",recordNo);
            resp.setData(contrail);
            resp.setMessage("查询成功");
        } catch (BikeException e) {
            resp.setCode(e.getStatusCode());
            resp.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Fail to query ride record ", e);
            resp.setCode(Constants.RESP_STATUS_INTERNAL_ERROR);
            resp.setMessage("内部错误");
        }

        return resp;
    }
}
