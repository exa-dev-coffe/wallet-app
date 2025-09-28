package com.time_tracker.be.utils.commons;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class GenericSpecification {

    public static <T> Specification<T> dynamicFilter(String key, String value) {
        return (root, query, cb) -> {
            if (key == null || value == null) {
                return cb.conjunction();
            }

            Class<?> fieldType = root.get(key).getJavaType();

            try {
                if (fieldType.equals(Integer.class)) {
                    log.info("Filtering non-string field: {} with value: {}", key, value);
                    int intValue = Integer.parseInt(value);
                    return cb.equal(root.get(key), intValue);

                } else if (fieldType.equals(String.class)) {
                    log.info("Filtering string field: {} with value: {}", key, value);
                    return cb.like(cb.lower(root.get(key).as(String.class)), "%" + value.toLowerCase() + "%");

                } else if (fieldType.equals(Boolean.class)) {
                    log.info("Filtering boolean field: {} with value: {}", key, value);
                    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                        boolean boolValue = Boolean.parseBoolean(value);
                        return cb.equal(root.get(key), boolValue);
                    } else {
                        log.error("Failed to parse value: {} to Boolean for key: {}", value, key);
                        return cb.disjunction();
                    }
                } else {
                    log.warn("Unsupported field type for key: {}", key);
                    return cb.disjunction();
                }
            } catch (NumberFormatException e) {
                log.error("Failed to parse value: {} for key: {}", value, key, e);
                // conjunction itu selalu true, jadi kalau parsing error, kita return false biar gaada data yang ke return
                // disjunction itu selalu false, kebalikan si conjunction
                return cb.disjunction();
            }
        };
    }


}
