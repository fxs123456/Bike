package com.fxs.bike.bicycle.service;

import com.fxs.bike.bicycle.entity.BikeLocation;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.user.entity.UserElement;

public interface BikeService {

    void generateBike() throws BikeException;

    void unLockBike(UserElement currentUser, Long number) throws BikeException;

    void lockBike(BikeLocation bikeLocation) throws BikeException;

    void reportLocation(BikeLocation bikeLocation) throws BikeException;
}
