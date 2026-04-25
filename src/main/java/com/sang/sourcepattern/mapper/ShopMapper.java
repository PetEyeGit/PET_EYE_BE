package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShopMapper {
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "staffs", ignore = true)
    @Mapping(target = "cages", ignore = true)
    @Mapping(target = "ratingAvg", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    Shop toShop(ShopRegistrationRequest request);

    @Mapping(target = "ownerId", source = "owner.id")
    ShopResponse toShopResponse(Shop shop);
}
