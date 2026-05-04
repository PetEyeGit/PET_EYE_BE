package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopCustomerResponse {
    long totalCustomers;
    long newCustomersThisMonth;
    long loyalCustomers;
    List<CustomerItemResponse> customers;
}
