package com.school.app.config;

import com.school.app.audit.Auditable;
import com.school.app.entity.SystemAuditLog;
import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración de carga inicial de datos (Data Seeding).
 * Esta clase se encarga de poblar la base de datos con usuarios base
 * necesarios para la operación inicial del sistema.
 * * <p><strong>Nota:</strong> Esta configuración está limitada al perfil 'dev'
 * para garantizar la seguridad en entornos de producción.</p>
 */
@Slf4j
@Configuration
//@Profile("dev")
public class DataSeedConfig {

    /**
     * Inicializa la base de datos con usuarios administradores y vendedores
     * por defecto si aún no existen en el sistema.
     *
     * @param userRepository  Repositorio para persistir los usuarios.
     * @param passwordEncoder Codificador para asegurar las contraseñas en BCrypt.
     * @return Un {@link CommandLineRunner} que se ejecuta al iniciar la aplicación.
     */
    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("🚀 Iniciando proceso de Data Seeding en el entorno de desarrollo...");
            // Llamamos a métodos delegados para que la auditoría sea capturada por el Proxy de Spring
            createDefaultUser(userRepository, passwordEncoder, "davhoAdmin", "davhorp90@gmail.com", "Jonathan", "Reyes", User.Role.ADMIN);
            createDefaultUser(userRepository, passwordEncoder, "davhoSeller", "davhorp9090@gmail.com", "David", "Ponce", User.Role.SELLER);
            log.info("🏁 Proceso de Data Seeding finalizado.");
        };
    }

    /**
     * Método delegado para crear usuarios.
     * La anotación @Auditable funcionará aquí si la clase es inyectada correctamente
     * o si el aspecto AOP está configurado para capturar métodos internos.
     */
    @Auditable(action = SystemAuditLog.AuditAction.VIEW_ADMIN_DASHBOARD, entityName = "USERS")
    private void createDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   String username, String email, String firstName, String lastName, User.Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(role)
                    .isActive(true)
                    .lastName(lastName)
                    .firstName(firstName)
                    .build();
            userRepository.save(user);
            log.info("✅ Usuario {} [{}] creado exitosamente.", role, username);
        } else {
            log.info("ℹ️ El usuario {} ya existe, omitiendo creación.", username);
        }
    }
}
