package com.wallet_service.be.utils.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PermissionActionDto implements Serializable {
    private boolean view;
    private boolean create;
    private boolean edit;
    private boolean delete;
}
