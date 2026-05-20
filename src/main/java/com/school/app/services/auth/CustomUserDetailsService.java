package com.school.app.services.auth;

import com.school.app.entity.User;
import com.school.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscamos el usuario en nuestra BD
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        // Validamos si el usuario fue dado de baja (isActive = false)
        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("El usuario está desactivado");
        }
        // Traducimos nuestra entidad al UserDetails de Spring
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(), // Spring usará el BCryptPasswordEncoder contra este hash
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
