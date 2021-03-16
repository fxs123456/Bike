package com.fxs.bike.bicycle.entity;

import lombok.Data;

@Data
public class BikeLocation {

    private String id;

    private Long bikeNumber;

    private int status;

    private Double[] coordinates;

    private Double distance;

}
