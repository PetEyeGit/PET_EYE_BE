package com.sang.sourcepattern.config;

import com.sang.sourcepattern.mapper.ShopMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicitly expose MapStruct mappers as Spring beans if component scanning
 * does not pick up the generated implementations in some environments.
 */
@Configuration
public class MapperConfig {

    @Bean
    public ShopMapper shopMapper() {
        return Mappers.getMapper(ShopMapper.class);
    }
}

