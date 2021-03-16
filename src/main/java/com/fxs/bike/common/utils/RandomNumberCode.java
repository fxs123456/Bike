package com.fxs.bike.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Random;

public class RandomNumberCode {

    public static String verCode() {
        Random random = new Random();
        return StringUtils.substring(String.valueOf(random.nextInt()), 2, 6);
    }

    public static String randomNo() {
        Random random = new Random();
        return String.valueOf(Math.abs(random.nextInt() * -10));
    }
    public static void main(String[] args) {
        Random random = new Random();
        System.out.println(random.nextInt() * -10);
    }
}
