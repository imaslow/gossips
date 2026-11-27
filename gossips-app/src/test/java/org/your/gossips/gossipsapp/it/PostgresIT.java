package org.your.gossips.gossipsapp.it;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * База для интеграционных тестов gossips-app: PostgreSQL в Docker поднимается один раз
 * на JVM (singleton-статик), Flyway накатывает V1–V5 на чистую БД, параметры подключения
 * пробрасываются в Spring через @DynamicPropertySource.
 *
 * Singleton вместо @Container: JUnit-расширение Testcontainers останавливает static-контейнер
 * после каждого тестового класса-наследника, а кэшированный Spring-контекст держит старый URL.
 * Контейнер, запущенный вручную, живёт до shutdown-hook Testcontainers — весь прогон на одном.
 */
@SpringBootTest
public abstract class PostgresIT {

    static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
