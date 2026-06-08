package com.ridehailing.location_service.service;

import com.ridehailing.location_service.model.request.DriverLocationRequest;
import com.ridehailing.location_service.model.response.DriverLocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Service
public class DriverLocationService {

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest){
 // post location to redis
    }


    public List<DriverLocationResponse> findNearbyDrivers( double latitude, double longitude, double radius){
        // call redis
        return Arrays.asList(new DriverLocationResponse());
    }
}
