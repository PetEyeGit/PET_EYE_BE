package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;

import java.util.List;

public interface ShopService {
    ShopResponse registerShop(ShopRegistrationRequest request);
    ShopResponse approveShop(int shopId);
    List<ShopResponse> getAllShops();
    ShopResponse getShopById(int id);
}
