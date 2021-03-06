package com.fxs.bike.wallet.dao;

import com.fxs.bike.wallet.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Wallet record);

    int insertSelective(Wallet record);

    Wallet selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Wallet record);

    int updateByPrimaryKey(Wallet record);

    Wallet selectByUserId(Long userId);
}