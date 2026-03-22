package com.badereddine.client.model;

/**
 * Data class representing a Role
 */
public class Role {
    private Integer id;
    private String name;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        if (name == null) return "";
        return name.replace("ROLE_", "");
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
