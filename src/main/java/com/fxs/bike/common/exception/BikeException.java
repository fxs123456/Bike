package com.fxs.bike.common.exception;


import com.fxs.bike.common.constants.Constants;

public class BikeException extends Exception {

    public BikeException(String message) {
        super(message);
    }
    public int getStatusCode() {
        return Constants.RESP_STATUS_INTERNAL_ERROR;
    }
}
