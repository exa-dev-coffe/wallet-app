package com.time_tracker.be.role;


import com.time_tracker.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@Table(name = "tm_roles")
@NoArgsConstructor
public class RoleModel extends BaseModal {
    @Id
    @Column(name = "role_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roleId;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;
}
