package com.lightspec.auth.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "bootstrap_guard")
public class BootstrapGuardEntity {

    @Id
    private Integer id;

    @Version
    private int version;

    protected BootstrapGuardEntity() {
    }

    public Integer getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }
}
