package com.onidza.backend.service.testcontainers;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@Testcontainers //isolation for each IT test class
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) //isolation for each IT test class
public abstract class AbstractITConfiguration {

    @MockitoBean
    protected RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    protected StringRedisTemplate stringRedisTemplate;

    @Container //isolation for each IT test class
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");

/*
isolation singleton - one container for all IT tests
    static {
        POSTGRES.start();
    }
*/

    @DynamicPropertySource
    static void datasourceProperty(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
