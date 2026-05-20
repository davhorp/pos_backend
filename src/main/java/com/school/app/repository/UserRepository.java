package com.school.app.repository;

import com.school.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Necesario para Spring Security (Auth)
    Optional<User> findByUsername(String username);

    // Útil para validar antes de crear un nuevo usuario desde el panel Admin
    boolean existsByUsername(String username);

    // Opcional: Para el panel administrativo, buscar solo usuarios activos
    List<User> findByIsActiveTrue();

}
