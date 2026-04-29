package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;

import java.util.List;

public interface ShopService {
    ShopResponse registerShop(ShopRegistrationRequest request);
    ShopResponse approveShop(int shopId);
    List<ShopResponse> getAllShops();
    ShopResponse getShopById(int id);

    /** Public: only verified shops, optional keyword/city/shopType filter */
    List<ShopResponse> searchVerifiedShops(String keyword, String city, String shopType);

    /** Public: get a single verified shop by id */
    ShopResponse getVerifiedShopById(int id);
}
