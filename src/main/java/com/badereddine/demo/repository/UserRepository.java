package com.badereddine.demo.repository;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Page<User> findAll(Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.company) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") ERole roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.enabled = true")
    long countActiveByRoleName(@Param("roleName") ERole roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countNewUsersSince(@Param("since") Date since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role.name = :roleName ORDER BY u.id")
    List<User> findActiveByRoleNameForUpdate(@Param("roleName") ERole roleName);

    Optional<User> findById(Long id);
}
