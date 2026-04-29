package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.ServiceCreationRequest;
import com.sang.sourcepattern.dto.request.ServiceUpdateRequest;
import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.entity.Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Service toService(ServiceCreationRequest request);

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.shopName")
    ServiceResponse toServiceResponse(Service service);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateService(@MappingTarget Service service, ServiceUpdateRequest request);
}
