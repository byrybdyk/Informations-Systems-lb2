package com.byrybdyk.lb1.repository;

import com.byrybdyk.lb1.model.User;
import com.byrybdyk.lb1.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

//    Long getUserIdByUsername(String username);

    long countByRole(Role role);

    Optional <String> findUsernameById(Long id);
}