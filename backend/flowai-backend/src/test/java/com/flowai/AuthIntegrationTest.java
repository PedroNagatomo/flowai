package com.flowai;

import com.flowai.dto.LoginRequest;
import com.flowai.dto.RegisterRequest;
import com.flowai.repository.UserRepository;
import com.flowai.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("flowai_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AuthService authService;
    @Autowired UserRepository userRepo;

    @Test
    void register_createsUserAndReturnsToken() {
        var res = authService.register(new RegisterRequest("alice@test.com", "senha123", "Alice"));
        assertThat(res.token()).isNotBlank();
        assertThat(res.email()).isEqualTo("alice@test.com");
        assertThat(userRepo.existsByEmail("alice@test.com")).isTrue();
    }

    @Test
    void register_duplicateEmail_throws() {
        authService.register(new RegisterRequest("bob@test.com", "senha123", "Bob"));
        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("bob@test.com", "outra", "Bob2"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_returnsToken() {
        authService.register(new RegisterRequest("carol@test.com", "senha123", "Carol"));
        var res = authService.login(new LoginRequest("carol@test.com", "senha123"));
        assertThat(res.token()).isNotBlank();
    }

    @Test
    void login_wrongPassword_throws() {
        authService.register(new RegisterRequest("dan@test.com", "senha123", "Dan"));
        assertThatThrownBy(() ->
                authService.login(new LoginRequest("dan@test.com", "errada"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}