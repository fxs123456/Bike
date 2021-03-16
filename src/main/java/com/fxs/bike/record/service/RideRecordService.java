package com.fxs.bike.record.service;

import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.record.entity.RideRecord;

import java.util.List;

public interface RideRecordService {

    List<RideRecord> listRideRecord(Long userId, Long lastId) throws BikeException;
}
