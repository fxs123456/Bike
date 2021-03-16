package com.fxs.bike.bicycle.service;

import com.baidu.yun.core.annotation.R;
import com.fxs.bike.bicycle.entity.BikeLocation;
import com.fxs.bike.bicycle.entity.Point;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.record.entity.RideContrail;
import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.DiskFileUpload;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

@Component
@Slf4j
public class BikeGeoService {

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<BikeLocation> geoNearSphere(String collection, String locationField,
                                            Point point, long minDistance, long maxDistance,
                                            DBObject query, DBObject fields, int limit) throws BikeException {
        try {
            if(query == null) {
                query = new BasicDBObject();
            }
            query.put(locationField,
                    new BasicDBObject("$nearSphere",
                            new BasicDBObject("$geometry",
                                    new BasicDBObject("type", "Point")
                                            .append("coordinates", new double[]{point.getLongitude(), point.getLatitude()}))
                                    .append("$minDistance", minDistance)
                                    .append("$maxDistance", maxDistance)
                    ));
            query.put("status", 1);

            FindIterable objList = mongoTemplate.getCollection(collection).find((Bson)query).limit(limit);
            List<BikeLocation> result = new ArrayList<>();
            MongoCursor<Document> cursor = objList.iterator();
            while(cursor.hasNext()) {
                BikeLocation bikeLocation = new BikeLocation();
                Document document = cursor.next();
                bikeLocation.setBikeNumber(document.getInteger("bike_no").longValue());
                bikeLocation.setStatus(document.getInteger("status"));
                Document cor = (Document)document.get("location");
                Double[] tmp = ((List<Double>)cor.get("coordinates")).toArray(new Double[0]);
                bikeLocation.setCoordinates(tmp);
                result.add(bikeLocation);
            }
            return result;
        } catch (Exception e) {
            log.error("fail to find around bike", e);
            throw new BikeException("查找附近单车失败");
        }
    }

    public List<BikeLocation> geoNear(String collection, DBObject query, Point point,
                                      int limit, long maxDistance) throws BikeException {
        try {
            if (query == null) {
                query = new BasicDBObject();
            }
            List<Bson> pipeLine = new ArrayList<>();
            BasicDBObject aggregate = new BasicDBObject("$geoNear",
                    new BasicDBObject("near", new BasicDBObject("type", "Point").append("coordinates", new double[]{point.getLongitude(), point.getLatitude()}))
                            .append("distanceField", "distance")
                            .append("num", limit)
                            .append("maxDistance", maxDistance)
                            .append("spherical", true)
                            .append("query", new BasicDBObject("status", 1))
            );
            pipeLine.add((Bson) aggregate);
            AggregateIterable<Document> objList = mongoTemplate.getCollection(collection).aggregate(pipeLine);
            MongoCursor<Document> cursor = objList.iterator();
            List<BikeLocation> result = new ArrayList<>();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                BikeLocation bikeLocation = new BikeLocation();
                bikeLocation.setBikeNumber(document.getInteger("bike_no").longValue());
                bikeLocation.setStatus(document.getInteger("status"));
                Document cor = (Document)document.get("location");
                Double[] tmp = ((List<Double>)cor.get("coordinates")).toArray(new Double[0]);
                bikeLocation.setCoordinates(tmp);
                bikeLocation.setDistance(document.getDouble("distance"));
                result.add(bikeLocation);
            }
            return result;
        } catch (Exception e) {
            log.error("fail to find around bike", e);
            throw new BikeException("查找附近单车失败");
        }
    }

    public RideContrail rideContrail(String collection, String recordNo) throws BikeException{
        try {
            BasicDBObject query = new BasicDBObject("record_no", recordNo);
            FindIterable<Document> obj = mongoTemplate.getCollection(collection).find((Bson) query);
            MongoCursor<Document> cursor = obj.iterator();
            Document document = cursor.next();
            RideContrail rideContrail = new RideContrail();
            rideContrail.setRideRecordNo(document.getString("record_no"));
            rideContrail.setBikeNo(document.getInteger("bike_no").longValue());
            List<Document> locList = (List<Document>) document.get("contrail");
            List<Point> pointList = new ArrayList<>();
            for(Document d : locList) {
                Double[] tmp = ((List<Double>)d.get("loc")).toArray(new Double[0]);
                Point point = new Point(tmp);
                pointList.add(point);
            }
            rideContrail.setContrail(pointList);
            return rideContrail;
        } catch (Exception e) {
            log.error("fail to query ride contrail", e);
            throw new BikeException("查询单车轨迹失败");
        }
    }
}
