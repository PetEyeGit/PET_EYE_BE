package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.mapper.ShopMapper;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ShopService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShopServiceImpl implements ShopService {

    ShopRepository shopRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    ShopMapper shopMapper;
    PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ShopResponse registerShop(ShopRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (shopRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.SHOP_EXISTED);
        }

        // 1. Create SHOP_OWNER user
        Role shopOwnerRole = roleRepository.findByName("SHOP_OWNER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(shopOwnerRole);

        User owner = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getShopName()) // Use shop name as full name for now
                .phone(request.getPhone())
                .address(request.getAddress())
                .roles(roles)
                .active(true) // Account is active, but shop needs verification
                .build();

        owner = userRepository.save(owner);

        // 2. Create Shop record
        Shop shop = shopMapper.toShop(request);
        shop.setOwner(owner);
        shop.setVerified(false); // Needs Admin approval

        shop = shopRepository.save(shop);

        log.info("Shop registered successfully: {} (Pending Approval)", shop.getShopName());

        return shopMapper.toShopResponse(shop);
    }

    @Override
    @Transactional
    public ShopResponse approveShop(int shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        shop.setVerified(true);
        
        shopRepository.save(shop);
        
        log.info("Shop approved by admin: {}", shop.getShopName());

        return shopMapper.toShopResponse(shop);
    }

    @Override
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAll().stream()
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public ShopResponse getShopById(int id) {
        return shopRepository.findById(id)
                .map(shopMapper::toShopResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
    }

    @Override
    public List<ShopResponse> searchVerifiedShops(String keyword, String city, String shopType) {
        return shopRepository.searchVerified(keyword, city, shopType)
                .stream()
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public ShopResponse getVerifiedShopById(int id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        if (!shop.isVerified()) {
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }
        return shopMapper.toShopResponse(shop);
    }
}
