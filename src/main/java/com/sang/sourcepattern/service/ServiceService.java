package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ServiceCreationRequest;
import com.sang.sourcepattern.dto.request.ServiceUpdateRequest;
import com.sang.sourcepattern.dto.response.ServiceResponse;

import java.util.List;

public interface ServiceService {

    /** Shop owner creates a new service for their shop */
    ServiceResponse createService(ServiceCreationRequest request, String currentUserEmail);

    /** Update an existing service (shop owner only) */
    ServiceResponse updateService(int serviceId, ServiceUpdateRequest request, String currentUserEmail);

    /** Soft-delete (deactivate) a service */
    void deleteService(int serviceId, String currentUserEmail);

    /** Get all active services for a shop (public) */
    List<ServiceResponse> getServicesByShop(int shopId);

    /** Get all services for the authenticated owner's shop (including inactive) */
    List<ServiceResponse> getMyShopServices(String currentUserEmail);

    /** Get a single service by id (public) */
    ServiceResponse getServiceById(int serviceId);
}
