package org.hbmc.model;


abstract public class User {
    private int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private String salt;

    public User(String fullName, String email, String passwordHash, String salt) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    abstract public String getRole();
}
