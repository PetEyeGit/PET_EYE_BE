package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SafetyFilterService {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (previous|all|above) instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget (everything|all|your instructions)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act as (a|an|if)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[SYSTEM\\]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[INST\\]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("DAN mode", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend you are", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard (all|previous|your)", Pattern.CASE_INSENSITIVE)
    );

    @Value("${ai.safety.max-input-length:2000}")
    private int maxInputLength;

    public void validate(String input) {
        if (input == null || input.isBlank()) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        if (input.length() > maxInputLength) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("[SafetyFilter] Potential prompt injection: {}",
                        input.substring(0, Math.min(100, input.length())));
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
    }}
