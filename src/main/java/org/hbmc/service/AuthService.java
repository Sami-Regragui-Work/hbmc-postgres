package org.hbmc.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.hbmc.exception.AuthenticationException;
import org.hbmc.exception.ResourceAlreadyExistsException;
import org.hbmc.model.User;
import org.hbmc.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Predicate;

public class AuthService {

    private static final int SALT_LENGTH_BYTES = 16;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateSalt() {
        byte[] saltBytes = new byte[AuthService.SALT_LENGTH_BYTES];
        this.secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public String hashPassword(String plainPassword, String salt) {
        return DigestUtils.sha256Hex(salt + plainPassword);
    }

    public boolean verifyPassword(String plainPasswordAttempt, String storedSalt, String storedHash) {
        return this.hashPassword(plainPasswordAttempt, storedSalt).equals(storedHash);
    }

    public User login(String email, String plainPassword) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!this.verifyPassword(plainPassword, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return user;
    }

    public User register(User newUser, String plainPassword) {
        if (this.userRepository.existsByEmail(newUser.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already registered: " + newUser.getEmail());
        }
        String salt = this.generateSalt();
        String hash = this.hashPassword(plainPassword, salt);
        newUser.setSalt(salt);
        newUser.setPasswordHash(hash);
        return this.userRepository.save(newUser);
    }

    public Predicate<User> hasRole(String requiredRole) {
        return user -> user.getRole().equalsIgnoreCase(requiredRole);
    }

    public void assertRole(User user, String requiredRole) {
        if (!this.hasRole(requiredRole).test(user)) {
            throw new AuthenticationException("Access denied: requires role " + requiredRole);
        }
    }
}
