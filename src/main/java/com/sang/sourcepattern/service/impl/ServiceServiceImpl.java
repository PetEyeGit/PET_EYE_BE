package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.ServiceCreationRequest;
import com.sang.sourcepattern.dto.request.ServiceUpdateRequest;
import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.entity.Service;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.mapper.ServiceMapper;
import com.sang.sourcepattern.repository.ServiceRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ServiceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ServiceServiceImpl implements ServiceService {

    ServiceRepository serviceRepository;
    ShopRepository shopRepository;
    UserRepository userRepository;
    ServiceMapper serviceMapper;

    // ─── helpers ────────────────────────────────────────────────────────────

    /** Resolve the Shop that belongs to the authenticated user. */
    private Shop resolveOwnerShop(String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (!shop.isVerified()) {
            throw new AppException(ErrorCode.SHOP_NOT_VERIFIED);
        }

        return shop;
    }

    /** Ensure the service belongs to the given shop. */
    private void assertServiceBelongsToShop(Service service, int shopId) {
        if (service.getShop().getId() != shopId) {
            throw new AppException(ErrorCode.SERVICE_NOT_BELONG_TO_SHOP);
        }
    }

    // ─── CRUD ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ServiceResponse createService(ServiceCreationRequest request, String currentUserEmail) {
        Shop shop = resolveOwnerShop(currentUserEmail);

        Service service = serviceMapper.toService(request);
        service.setShop(shop);

        service = serviceRepository.save(service);
        log.info("Service created: '{}' for shop '{}'", service.getServiceName(), shop.getShopName());

        return serviceMapper.toServiceResponse(service);
    }

    @Override
    @Transactional
    public ServiceResponse updateService(int serviceId, ServiceUpdateRequest request, String currentUserEmail) {
        Shop shop = resolveOwnerShop(currentUserEmail);

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

        assertServiceBelongsToShop(service, shop.getId());

        serviceMapper.updateService(service, request);
        service = serviceRepository.save(service);

        log.info("Service updated: id={} for shop '{}'", serviceId, shop.getShopName());
        return serviceMapper.toServiceResponse(service);
    }

    @Override
    @Transactional
    public void deleteService(int serviceId, String currentUserEmail) {
        Shop shop = resolveOwnerShop(currentUserEmail);

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));

        assertServiceBelongsToShop(service, shop.getId());

        service.setActive(false);
        serviceRepository.save(service);

        log.info("Service deactivated: id={} for shop '{}'", serviceId, shop.getShopName());
    }

    @Override
    public List<ServiceResponse> getServicesByShop(int shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        if (!shop.isVerified() || shop.getOwner() == null || !shop.getOwner().isActive()) {
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }
        // Public: only active services
        return serviceRepository.findByShopIdAndActiveTrue(shopId)
                .stream()
                .map(serviceMapper::toServiceResponse)
                .toList();
    }

    @Override
    public List<ServiceResponse> getMyShopServices(String currentUserEmail) {
        Shop shop = resolveOwnerShop(currentUserEmail);
        return serviceRepository.findByShopId(shop.getId())
                .stream()
                .map(serviceMapper::toServiceResponse)
                .toList();
    }

    @Override
    public ServiceResponse getServiceById(int serviceId) {
        return serviceRepository.findById(serviceId)
                .map(serviceMapper::toServiceResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));
    }
}
