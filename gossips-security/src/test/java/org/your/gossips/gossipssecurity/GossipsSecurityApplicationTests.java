package org.your.gossips.gossipssecurity;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        // Контекст без инфраструктуры БД: интеграционные тесты с БД — отдельно (Testcontainers)
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration,"
                + "org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration"
})
class GossipsSecurityApplicationTests {

    @MockitoBean
    private DSLContext dslContext;

    @Test
    void contextLoads() {
    }

}
