package org.hbmc.model;

public class Admin extends User {
    public Admin(String fullName, String email, String passwordHash, String salt) {
        super(fullName, email, passwordHash, salt);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
