package com.time_tracker.be.utils.commons;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseModal {
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Date createdAt;

    @Column(name = "created_by", nullable = true)
    private Integer createdBy;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    private Date updatedAt;

    @Column(name = "updated_by", nullable = true)
    private Integer updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }
}