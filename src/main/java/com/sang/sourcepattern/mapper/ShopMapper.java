package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.ShopCreationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Shop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShopMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "staffs", ignore = true)
    @Mapping(target = "cages", ignore = true)
    @Mapping(target = "ratingAvg", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    Shop toShop(ShopCreationRequest request);

    @Mapping(target = "ownerFullName", source = "owner.fullName")
    ShopResponse toShopResponse(Shop shop);
}
