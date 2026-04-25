package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ShopCreationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Shop;
import java.util.List;

public interface ShopService {
    Shop createShop(Shop shop);
    ShopResponse createShop(ShopCreationRequest request);
    List<Shop> getAllShops();
    List<ShopResponse> getAllShopResponses();
    Shop getShopById(int id);
}
