package com.fxs.bike.record.entity;

import com.fxs.bike.bicycle.entity.Point;
import lombok.Data;

import java.util.List;

@Data
public class RideContrail {

    private String rideRecordNo;

    private Long bikeNo;

    private List<Point> contrail;

}