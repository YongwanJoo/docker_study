package com.msa.auth_service.config;

import com.msa.auth_service.domain.User;
import com.msa.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if test user exists, if not create one
        if (userRepository.findByUsername("user").isEmpty()) {
            User testUser = new User("user", passwordEncoder.encode("password"), "ROLE_USER");
            userRepository.save(testUser);
            System.out.println("Initialized setup: Created default user 'user' with password 'password'");
        }
    }
}
