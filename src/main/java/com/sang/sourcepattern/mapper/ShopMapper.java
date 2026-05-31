package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.request.ShopUpdateRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Shop;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ShopMapper {

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "staffs", ignore = true)
    @Mapping(target = "cages", ignore = true)
    @Mapping(target = "ratingAvg", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "openTime", ignore = true)
    @Mapping(target = "closeTime", ignore = true)
    @Mapping(target = "workingDays", ignore = true)
    @Mapping(target = "assignmentMode", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "lateGracePeriod", ignore = true)
    Shop toShop(ShopRegistrationRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "isVerified", source = "verified")
    @Mapping(target = "status", expression = "java(shop.getStatus() != null ? shop.getStatus().name() : null)")
    ShopResponse toShopResponse(Shop shop);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "staffs", ignore = true)
    @Mapping(target = "cages", ignore = true)
    @Mapping(target = "ratingAvg", ignore = true)
    @Mapping(target = "verified", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "licenseNumber", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "lateGracePeriod", ignore = true)
    void updateShop(@MappingTarget Shop shop, ShopUpdateRequest request);
}
