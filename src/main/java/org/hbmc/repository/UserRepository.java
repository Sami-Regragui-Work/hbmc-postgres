package org.hbmc.repository;

import org.hbmc.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    User update(User user);
    void updatePassword(int userId, String newPasswordHash, String newSalt);
    Optional<User> findById(int id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAll();
}
