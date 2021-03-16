package com.fxs.bike.record.dao;

import com.fxs.bike.record.entity.RideRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RideRecordMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RideRecord record);

    int insertSelective(RideRecord record);

    RideRecord selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RideRecord record);

    int updateByPrimaryKey(RideRecord record);

    RideRecord selectRecordNotClosed(Long userId);

    RideRecord selectBikeRecordOnGoing(Long bikeNo);

    List<RideRecord> selectRideRecordPage(Long userId, Long lastId);
}