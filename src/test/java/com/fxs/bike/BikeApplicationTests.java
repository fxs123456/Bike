package com.fxs.bike;

import com.fxs.bike.bicycle.entity.BikeLocation;
import com.fxs.bike.bicycle.entity.Point;
import com.fxs.bike.bicycle.service.BikeGeoService;
import com.fxs.bike.bicycle.service.BikeService;
import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.user.entity.UserElement;
import com.fxs.bike.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@Slf4j
@SpringBootTest(classes = BikeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BikeApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private BikeGeoService bikeGeoService;

    @Autowired
    private BikeService bikeService;

    @Test
    void contextLoads() {
        String result = restTemplate.getForObject("/user/hello", String.class);
        System.out.println(result);
    }

    @Test
    void geoTest() throws BikeException {
        System.out.println(bikeGeoService.geoNearSphere("bike_position", "location",
                new Point(104.063339, 30.547347), 0, 50, null, null, 10));
        System.out.println(bikeGeoService.geoNear("bike_position", null, new Point(104.063339, 30.547347), 10, 50));
    }

    @Test
    void unlockTest() throws BikeException {
        UserElement ue = new UserElement();
        ue.setUserId(1l);
        ue.setPlatform("android");
        ue.setPushChannelId("12345");
        //bikeService.unLockBike(ue, 28000002l);
        BikeLocation bl = new BikeLocation();
        bl.setBikeNumber(28000002l);
        bl.setCoordinates(new Double[]{104.06356,
                30.547778});
        bikeService.lockBike(bl);
    }

    @Test
    void testReport() throws BikeException {
        BikeLocation bl = new BikeLocation();
        bl.setBikeNumber(28000002l);
        bl.setCoordinates(new Double[]{104.00000,
                30.0000});
        bikeService.reportLocation(bl);
    }

    @Test
    void testContrail() throws BikeException {
        bikeGeoService.rideContrail("ride_contrail", "16104390100611736585536");
    }
}
