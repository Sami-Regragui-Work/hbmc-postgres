package org.hbmc.model;

public class Client extends User {
    private String phone;

    public Client(String fullName, String email, String passwordHash, String salt, String phone) {
        super(fullName, email, passwordHash, salt);
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getRole() {
        return "CLIENT";
    }
}
