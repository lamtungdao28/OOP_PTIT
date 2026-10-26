package com.example.oopPTIT.repository;

import com.example.oopPTIT.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Modifying
    @Query("update User u set u.password = :newHash, u.updatedAt = :now where u.id = :id and u.password = :oldHash")
    int upgradePassword(Long id, String oldHash, String newHash, LocalDateTime now);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
