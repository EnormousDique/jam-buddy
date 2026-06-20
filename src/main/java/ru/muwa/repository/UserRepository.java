package ru.muwa.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.muwa.entity.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {


    boolean existsByTelegramId(Long telegramId);
    boolean existsByTelegramUsername(String telegramUsername);

    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findByTelegramUsername(String telegramUsername);

}
