package org.hbmc.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.hbmc.model.User;
import org.hbmc.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Predicate;

public class AuthService {

    private static final int SALT_LENGTH_BYTES = 16;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public String hashPassword(String plainPassword, String salt) {
        return DigestUtils.sha256Hex(salt + plainPassword);
    }

    public boolean verifyPassword(String plainPasswordAttempt, String storedSalt, String storedHash) {
        return hashPassword(plainPasswordAttempt, storedSalt).equals(storedHash);
    }

    public Optional<User> login(String email, String plainPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        boolean valid = verifyPassword(plainPassword, user.getSalt(), user.getPasswordHash());
        return valid ? Optional.of(user) : Optional.empty();
    }

    public User register(User newUser, String plainPassword) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + newUser.getEmail());
        }
        String salt = generateSalt();
        String hash = hashPassword(plainPassword, salt);
        newUser.setSalt(salt);
        newUser.setPasswordHash(hash);
        return userRepository.save(newUser);
    }

    public Predicate<User> hasRole(String requiredRole) {
        return user -> user.getRole().equalsIgnoreCase(requiredRole);
    }
}