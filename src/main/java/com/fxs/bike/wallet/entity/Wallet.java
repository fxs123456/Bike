package com.fxs.bike.wallet.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Wallet {
    private Long id;

    private Long userid;

    private BigDecimal remainSum;

    private BigDecimal deposit;


}