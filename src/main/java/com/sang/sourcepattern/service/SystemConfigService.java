package com.sang.sourcepattern.service;

public interface SystemConfigService {
    boolean isVoucherServiceEnabled();
    void setVoucherServiceEnabled(boolean enabled);
}
