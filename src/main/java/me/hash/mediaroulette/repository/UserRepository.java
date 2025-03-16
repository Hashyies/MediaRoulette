package me.hash.mediaroulette.repository;

import me.hash.mediaroulette.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String userId);
    User save(User user);
    boolean exists(String userId);
}
