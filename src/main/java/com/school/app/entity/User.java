package com.school.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column
    private String firstName;
    @Column
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Puedes usar un Enum en Java y mapearlo a String en la BD
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // OffsetDateTime mapea correctamente a TIMESTAMP WITH TIME ZONE
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Enum interno para mayor legibilidad
    public enum Role {
        ADMIN, SELLER
    }

    /**
     * Genera el nombre completo dinámicamente.
     * No se guarda en la base de datos.
     */
    public String getFullName() {
        return String.join(" ",
                this.firstName != null ? this.firstName : "",
                this.lastName != null ? this.lastName : ""
        ).trim();
    }
}
