package org.hbmc.model;

public class Client extends User {
    private String phone;

    public Client(String fullName, String email, String passwordHash, String phone) {
        super(fullName, email, passwordHash);
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
        return "Client";
    }
}
