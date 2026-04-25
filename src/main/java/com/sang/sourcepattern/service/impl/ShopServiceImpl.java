package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.ShopCreationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.mapper.ShopMapper;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ShopService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopServiceImpl implements ShopService {
    ShopRepository shopRepository;
    UserRepository userRepository;
    ShopMapper shopMapper;

    @Override
    public Shop createShop(Shop shop) {
        return shopRepository.save(shop);
    }

    public ShopResponse createShop(ShopCreationRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Shop shop = shopMapper.toShop(request);
        shop.setOwner(owner);
        
        return shopMapper.toShopResponse(shopRepository.save(shop));
    }

    @Override
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    public List<ShopResponse> getAllShopResponses() {
        return shopRepository.findAll().stream()
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public Shop getShopById(int id) {
        return shopRepository.findById(id).orElseThrow(() -> new RuntimeException("Shop not found"));
    }
}
