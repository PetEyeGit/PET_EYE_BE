package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.response.goong.LatLong;
import com.sang.sourcepattern.dto.response.goong.GoongDirectionsResponse;

public interface GoongMapService {
    
    /**
     * Chuyển địa chỉ thành tọa độ GPS (lat, lng)
     */
    LatLong geocodeAddress(String address);
    
    /**
     * Tính khoảng cách giữa 2 điểm (km)
     */
    double getDistanceKm(LatLong origin, LatLong destination);
    
    /**
     * Lấy thông tin chỉ đường từ origin đến destination
     */
    GoongDirectionsResponse getDirections(LatLong origin, LatLong destination);
}
