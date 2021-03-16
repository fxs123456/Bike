package com.fxs.bike.bicycle.dao;

import com.fxs.bike.bicycle.entity.Bicycle;
import com.fxs.bike.bicycle.entity.BikeNoGen;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BicycleMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Bicycle record);

    int insertSelective(Bicycle record);

    Bicycle selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Bicycle record);

    /**
     * 生成唯一单车编号sql
     * @param bikeNoGen
     */
    void generateBikeNo(BikeNoGen bikeNoGen);

    int updateByPrimaryKey(Bicycle record);

    Bicycle selectByBikeNo(Long bikeNo);
}