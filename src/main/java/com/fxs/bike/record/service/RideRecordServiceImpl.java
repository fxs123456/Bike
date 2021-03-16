package com.fxs.bike.record.service;

import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.record.dao.RideRecordMapper;
import com.fxs.bike.record.entity.RideRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RideRecordServiceImpl implements RideRecordService{

    @Autowired
    private RideRecordMapper rideRecordMapper;

    @Override
    public List<RideRecord> listRideRecord(Long userId, Long lastId) throws BikeException {
        List<RideRecord> list = rideRecordMapper.selectRideRecordPage(userId,lastId);
        return list;
    }
}
