package org.hbmc.model;

public class Admin extends User {
    public Admin(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}
