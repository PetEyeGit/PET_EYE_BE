package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.request.ShopUpdateRequest;
import com.sang.sourcepattern.dto.response.CustomerDetailResponse;
import com.sang.sourcepattern.dto.response.ShopCustomerResponse;
import com.sang.sourcepattern.dto.response.ShopDashboardResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;

import com.sang.sourcepattern.dto.response.PageResponse;
import java.util.List;

public interface ShopService {
    ShopCustomerResponse getShopCustomers(String ownerEmail);
    ShopDashboardResponse getShopDashboard(String ownerEmail, java.time.LocalDate startDate, java.time.LocalDate endDate);
    CustomerDetailResponse getCustomerDetail(String ownerEmail, int customerId);
    ShopResponse registerShop(ShopRegistrationRequest request);
    ShopResponse approveShop(int shopId);
    List<ShopResponse> getAllShops();
    PageResponse<ShopResponse> getAllShopsPaged(int page);
    ShopResponse getShopById(int id);

    ShopResponse getMyShop(String email);
    ShopResponse updateMyShop(String email, ShopUpdateRequest request);

    /** Public: only verified shops, optional keyword/city/shopType filter */
    List<ShopResponse> searchVerifiedShops(String keyword, String city, String shopType);
    PageResponse<ShopResponse> searchVerifiedShopsPaged(String keyword, String city, String shopType, int page, int size);
    PageResponse<ShopResponse> searchVerifiedShopsPaged(String keyword, String city, String shopType, int page);

    /** Public: get a single verified shop by id */
    ShopResponse getVerifiedShopById(int id);

    /** Admin: reject/deactivate a shop */
    void rejectShop(int shopId, String reason);

    /** Admin: get shops pending verification */
    List<ShopResponse> getPendingShops();

    /** Admin: get staff of a shop */
    List<com.sang.sourcepattern.dto.response.StaffResponse> getStaffByShop(int shopId);

    /** Public: search nearby shops by location and radius */
    List<com.sang.sourcepattern.dto.response.ShopNearbyResponse> searchNearbyShops(
            Double latitude, Double longitude, Double radiusKm);

    /** Public: get directions from user location to shop */
    com.sang.sourcepattern.dto.response.goong.GoongDirectionsResponse getDirectionsToShop(
            int shopId, Double fromLat, Double fromLng);
}
