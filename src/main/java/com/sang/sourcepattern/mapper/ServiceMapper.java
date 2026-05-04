package com.sang.sourcepattern.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.sourcepattern.dto.request.ServiceCreationRequest;
import com.sang.sourcepattern.dto.request.ServiceUpdateRequest;
import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.entity.Service;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    ObjectMapper JSON = new ObjectMapper();

    // ── List<String> ↔ JSON string helpers ───────────────────────────────────

    default String listToJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try { return JSON.writeValueAsString(list); } catch (Exception e) { return null; }
    }

    default List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try { return JSON.readValue(json, new TypeReference<List<String>>() {}); } catch (Exception e) { return Collections.emptyList(); }
    }

    // ── Map<String,Integer> ↔ JSON string helpers ─────────────────────────────

    default String mapIntToJson(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return null;
        try { return JSON.writeValueAsString(map); } catch (Exception e) { return null; }
    }

    default Map<String, Integer> jsonToMapInt(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try { return JSON.readValue(json, new TypeReference<Map<String, Integer>>() {}); } catch (Exception e) { return Collections.emptyMap(); }
    }

    // ── Map<String,String> ↔ JSON string helpers ──────────────────────────────

    default String mapStrToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try { return JSON.writeValueAsString(map); } catch (Exception e) { return null; }
    }

    default Map<String, String> jsonToMapStr(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try { return JSON.readValue(json, new TypeReference<Map<String, String>>() {}); } catch (Exception e) { return Collections.emptyMap(); }
    }

    // ── toService ─────────────────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "cameraTiers",      expression = "java(listToJson(request.getCameraTiers()))")
    @Mapping(target = "cameraTierPrices", expression = "java(mapIntToJson(request.getCameraTierPrices()))")
    @Mapping(target = "cameraTierLabels", expression = "java(mapStrToJson(request.getCameraTierLabels()))")
    Service toService(ServiceCreationRequest request);

    // ── toServiceResponse ─────────────────────────────────────────────────────

    @Mapping(target = "shopId",           source = "shop.id")
    @Mapping(target = "shopName",         source = "shop.shopName")
    @Mapping(target = "cameraTiers",      expression = "java(jsonToList(service.getCameraTiers()))")
    @Mapping(target = "cameraTierPrices", expression = "java(jsonToMapInt(service.getCameraTierPrices()))")
    @Mapping(target = "cameraTierLabels", expression = "java(jsonToMapStr(service.getCameraTierLabels()))")
    ServiceResponse toServiceResponse(Service service);

    // ── updateService ─────────────────────────────────────────────────────────

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "shop",             ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "cameraTiers",      expression = "java(request.getCameraTiers() != null ? listToJson(request.getCameraTiers()) : service.getCameraTiers())")
    @Mapping(target = "cameraTierPrices", expression = "java(request.getCameraTierPrices() != null ? mapIntToJson(request.getCameraTierPrices()) : service.getCameraTierPrices())")
    @Mapping(target = "cameraTierLabels", expression = "java(request.getCameraTierLabels() != null ? mapStrToJson(request.getCameraTierLabels()) : service.getCameraTierLabels())")
    void updateService(@MappingTarget Service service, ServiceUpdateRequest request);
}
