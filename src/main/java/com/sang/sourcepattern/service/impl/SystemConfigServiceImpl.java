package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.entity.SystemConfig;
import com.sang.sourcepattern.repository.SystemConfigRepository;
import com.sang.sourcepattern.service.SystemConfigService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemConfigServiceImpl implements SystemConfigService {

    SystemConfigRepository systemConfigRepository;

    private static final String VOUCHER_SERVICE_ENABLED = "VOUCHER_SERVICE_ENABLED";

    @Override
    public boolean isVoucherServiceEnabled() {
        Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(VOUCHER_SERVICE_ENABLED);
        if (configOpt.isEmpty()) {
            return true; // Default to true if not configured
        }
        return "true".equalsIgnoreCase(configOpt.get().getConfigValue());
    }

    @Override
    public void setVoucherServiceEnabled(boolean enabled) {
        SystemConfig config = systemConfigRepository.findByConfigKey(VOUCHER_SERVICE_ENABLED)
                .orElse(SystemConfig.builder().configKey(VOUCHER_SERVICE_ENABLED).build());
        config.setConfigValue(String.valueOf(enabled));
        systemConfigRepository.save(config);
    }
}
