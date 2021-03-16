package com.fxs.bike.bicycle.service;

import com.baidu.yun.core.annotation.R;
import com.fxs.bike.bicycle.dao.BicycleMapper;
import com.fxs.bike.bicycle.entity.Bicycle;
import com.fxs.bike.bicycle.entity.BikeLocation;
import com.fxs.bike.bicycle.entity.BikeNoGen;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.common.utils.BaiduPushUtil;
import com.fxs.bike.common.utils.DateUtil;
import com.fxs.bike.common.utils.RandomNumberCode;
import com.fxs.bike.fee.dao.RideFeeMapper;
import com.fxs.bike.fee.entity.RideFee;
import com.fxs.bike.record.dao.RideRecordMapper;
import com.fxs.bike.record.entity.RideRecord;
import com.fxs.bike.user.dao.UserMapper;
import com.fxs.bike.user.entity.User;
import com.fxs.bike.user.entity.UserElement;
import com.fxs.bike.wallet.dao.WalletMapper;
import com.fxs.bike.wallet.entity.Wallet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class BikeServiceImpl implements BikeService {

    private static final Byte NOT_VERIFY = 1;
    private static final Object BIKE_UNLOCK = 2;
    private static final Byte RIDE_END = 2;
    private static final Object BIKE_LOCK = 1;

    @Autowired
    private BicycleMapper bicycleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RideRecordMapper rideRecordMapper;

    @Autowired
    private WalletMapper walletMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RideFeeMapper rideFeeMapper;

    @Override
    public void generateBike() throws BikeException {
        //单车编号生成
        BikeNoGen bikeNoGen = new BikeNoGen();
        bicycleMapper.generateBikeNo(bikeNoGen);
        // 生成单车
        Bicycle bike = new Bicycle();
        bike.setType((byte)1);
        bike.setNumber(bikeNoGen.getAutoIncNo());
        bicycleMapper.insertSelective(bike);
    }

    @Override
    @Transactional
    public void unLockBike(UserElement currentUser, Long bikeNo) throws BikeException {
        try {
            //校验用户是否已经认证（实名认证 押金）
            User user = userMapper.selectByPrimaryKey(currentUser.getUserId());
            if(user.getVerifyFlag() == NOT_VERIFY) {
                throw new BikeException("用户尚未认证");
            }
            //检查用户是否有正在进行的骑行记录
            RideRecord record = rideRecordMapper.selectRecordNotClosed(currentUser.getUserId());
            if(record != null) {
                throw new BikeException("存在未关闭骑行订单");
            }
            //检查用户余额
            Wallet wallet = walletMapper.selectByUserId(currentUser.getUserId());
            if(wallet.getRemainSum().compareTo(new BigDecimal(1)) < 0) {
                throw new BikeException("余额不足");
            }
            //推送单车 进行解锁
            //BaiduPushUtil.pushMsgToSingleDevice(currentUser,"{\"title\":\"TEST\",\"description\":\"Hello Baidu push!\"}");
            //以下两步应该看作一个事务，但是由于数据源不同，无法实现
            //修改mongoDB中单车状态
            Query query = Query.query(Criteria.where("bike_no").is(bikeNo));
            Update update = Update.update("status", BIKE_UNLOCK);
            mongoTemplate.updateFirst(query, update, "bike_position");
            //建立骑行订单 记录骑行开始时间（记录骑行轨迹 单车上报坐标）
            RideRecord rideRecord = new RideRecord();
            rideRecord.setUserid(currentUser.getUserId());
            rideRecord.setBikeNo(bikeNo);
            String recordNo = new Date().getTime() + RandomNumberCode.randomNo();
            rideRecord.setRecordNo(recordNo);
            rideRecord.setStartTime(new Date());
            rideRecordMapper.insertSelective(rideRecord);
        } catch (Exception e) {
            log.error("Fail to unlock bike", e);
            throw new BikeException("解锁单车失败");
        }
    }

    @Override
    @Transactional
    public void lockBike(BikeLocation bikeLocation) throws BikeException {
        try {
            //结束订单，计算骑行时间存订单（根据bikeNO找到订单再找到UserID）
            RideRecord record = rideRecordMapper.selectBikeRecordOnGoing(bikeLocation.getBikeNumber());
            if(record == null) {
                throw new BikeException("骑行记录不存在");
            }
            Long userId = record.getUserid();
            //查询费用信息(根据单车类型）
            Bicycle bike = bicycleMapper.selectByBikeNo(bikeLocation.getBikeNumber());
            if(bike == null) {
                throw new BikeException("单车不存在");
            }
            RideFee rideFee = rideFeeMapper.selectBikeTypeFee(bike.getType());
            if(rideFee == null) {
                throw new BikeException("计费信息异常");
            }
            BigDecimal cost = BigDecimal.ZERO;
            record.setEndTime(new Date());
            record.setStatus(RIDE_END);
            Long min = DateUtil.getBetweenMin(record.getEndTime(), record.getStartTime());
            record.setRideTime(min.intValue());
            int minUnit = rideFee.getMinUnit();
            int intMin = min.intValue();
            if(intMin / minUnit==0){
                //不足一个时间单位 按照一个时间单位算
                cost = rideFee.getFee();
            }else if(intMin % minUnit==0){
                //整除了时间单位 直接计费
                cost = rideFee.getFee().multiply(new BigDecimal(intMin/minUnit));
            }else if(intMin % minUnit!=0){
                //不整除 +1 补足一个时间单位
                cost = rideFee.getFee().multiply(new BigDecimal((intMin/minUnit)+1));
            }
            record.setRideCost(cost);
            rideRecordMapper.updateByPrimaryKeySelective(record);
            //钱包扣费
            Wallet wallet = walletMapper.selectByUserId(userId);
            wallet.setRemainSum(wallet.getRemainSum().subtract(cost));
            walletMapper.updateByPrimaryKeySelective(wallet);
            //修改mongoDB中单车状态为锁定
            Query query = Query.query(Criteria.where("bike_no").is(bikeLocation.getBikeNumber()));
            Update update = Update.update("status",BIKE_LOCK)
                    .set("location.coordinates",bikeLocation.getCoordinates());
            mongoTemplate.updateFirst(query, update, "bike_position");
        } catch (Exception e) {
            log.error("fail to lock bike", e);
            throw new BikeException("锁定单车失败");
        }
    }

    @Override
    public void reportLocation(BikeLocation bikeLocation) throws BikeException {
        try {
            //在数据库中查询该单车进行中的订单
            RideRecord record = rideRecordMapper.selectBikeRecordOnGoing(bikeLocation.getBikeNumber());
            if(record == null) {
                throw new BikeException("骑行记录不存在");
            }
            //查询mongoDB是否是第一次上传坐标
            //第一次上传会在ride_contrail插入一条记录，以后的上传只会插入一个坐标

        BasicDBObject q = new BasicDBObject("record_no",record.getRecordNo());
        FindIterable<Document> obj = mongoTemplate.getCollection("ride_contrail").find((Bson)q);
        MongoCursor<Document> cursor = obj.iterator();
            if(!cursor.hasNext()) {
                List<BasicDBObject> list = new ArrayList();
                BasicDBObject temp = new BasicDBObject("loc",bikeLocation.getCoordinates());
                list.add(temp);
                BasicDBObject insertObj = new BasicDBObject("record_no",record.getRecordNo())
                        .append("bike_no",record.getBikeNo())
                        .append("contrail",list);
                mongoTemplate.insert(insertObj,"ride_contrail");
            }
            else {
                Query query = new Query(Criteria.where("record_no").is(record.getRecordNo()));
                Update update = new Update().push("contrail", new BasicDBObject("loc",bikeLocation.getCoordinates()));
                mongoTemplate.updateFirst(query,update,"ride_contrail");
            }
        } catch (Exception e) {
            log.error("fail to report location", e);
            throw new BikeException("上报坐标失败");
        }
    }
}
