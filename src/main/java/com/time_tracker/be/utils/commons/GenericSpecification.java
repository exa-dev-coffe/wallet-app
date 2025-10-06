package com.time_tracker.be.utils.commons;

import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class GenericSpecification {

    public static <T> Specification<T> dynamicFilter(String key, String value) {
        return (root, query, cb) -> {
            String[] keys = key.split(",");
            String[] values = value.split(",");

            // pastikan jumlah key dan value sama
            if (keys.length != values.length) {
                log.warn("Jumlah keys ({}) tidak sama dengan values ({})", keys.length, values.length);
                return cb.disjunction(); // selalu false
            }

            List<Predicate> predicates = new ArrayList<>();

            for (int i = 0; i < keys.length; i++) {
                String currentKey = keys[i].trim();
                String currentValue = values[i].trim();

                try {
                    Class<?> fieldType = root.get(currentKey).getJavaType();

                    if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                        log.info("Filtering integer field: {} with value: {}", currentKey, currentValue);
                        int intValue = Integer.parseInt(currentValue);
                        predicates.add(cb.equal(root.get(currentKey), intValue));

                    } else if (fieldType.equals(String.class)) {
                        log.info("Filtering string field: {} with value: {}", currentKey, currentValue);
                        predicates.add(cb.like(cb.lower(root.get(currentKey).as(String.class)), "%" + currentValue.toLowerCase() + "%"));

                    } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                        log.info("Filtering boolean field: {} with value: {}", currentKey, currentValue);
                        if (currentValue.equalsIgnoreCase("true") || currentValue.equalsIgnoreCase("false")) {
                            boolean boolValue = Boolean.parseBoolean(currentValue);
                            predicates.add(cb.equal(root.get(currentKey), boolValue));
                        } else {
                            log.error("Failed to parse value: {} to Boolean for key: {}", currentValue, currentKey);
                            return cb.disjunction();
                        }

                    } else {
                        log.warn("Unsupported field type for key: {}", currentKey);
                        return cb.disjunction();
                    }
                } catch (NumberFormatException e) {
                    log.error("Failed to parse value: {} for key: {}", currentValue, currentKey, e);
                    return cb.disjunction();
                } catch (IllegalArgumentException e) {
                    log.error("Invalid field name: {}", currentKey);
                    return cb.disjunction();
                }
            }

            // Gabungkan semua predicate dengan AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}
