package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.request.ShopUpdateRequest;
import com.sang.sourcepattern.dto.response.CustomerDetailResponse;
import com.sang.sourcepattern.dto.response.ShopCustomerResponse;
import com.sang.sourcepattern.dto.response.ShopDashboardResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;

import java.util.List;

public interface ShopService {
    ShopCustomerResponse getShopCustomers(String ownerEmail);
    ShopDashboardResponse getShopDashboard(String ownerEmail);
    CustomerDetailResponse getCustomerDetail(String ownerEmail, int customerId);
    ShopResponse registerShop(ShopRegistrationRequest request);
    ShopResponse approveShop(int shopId);
    List<ShopResponse> getAllShops();
    ShopResponse getShopById(int id);

    ShopResponse getMyShop(String email);
    ShopResponse updateMyShop(String email, ShopUpdateRequest request);

    /** Public: only verified shops, optional keyword/city/shopType filter */
    List<ShopResponse> searchVerifiedShops(String keyword, String city, String shopType);

    /** Public: get a single verified shop by id */
    ShopResponse getVerifiedShopById(int id);
}
