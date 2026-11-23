package org.your.gossips.gossipsapp;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        // Контекст без инфраструктуры БД: интеграционные тесты с БД — отдельно (Testcontainers)
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration"
})
class GossipsAppApplicationTests {

    @MockitoBean
    private DSLContext dslContext;

    @Test
    void contextLoads() {
    }

}
