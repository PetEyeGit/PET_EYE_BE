package com.sang.sourcepattern.service.ai.provider;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FunctionCall {
    String name;
    Map<String, Object> args;
}
