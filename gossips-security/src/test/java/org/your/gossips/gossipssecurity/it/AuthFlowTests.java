package org.your.gossips.gossipssecurity.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.your.gossips.common.ApiPaths;
import org.your.gossips.common.dto.AuthResponse;
import org.your.gossips.common.dto.RefreshTokenRequest;
import org.your.gossips.common.dto.UserCredentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной auth-flow security-сервиса: регистрация, логин, ротация и отзыв refresh-токенов,
 * коды ошибок ErrorCode. У каждого теста свои уникальные юзернеймы (общая БД на JVM).
 */
class AuthFlowTests extends PostgresIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerIssuesTokenPair() throws Exception {
        AuthResponse response = readBody(register("reg-user", "password-123")
                .andExpect(status().isOk()), AuthResponse.class);

        assertThat(response.getUsername()).isEqualTo("reg-user");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void duplicateRegisterIsConflict() throws Exception {
        register("dup-user", "password-123")
                .andExpect(status().isOk());

        register("dup-user", "other-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        register("login-user", "password-123");

        postJson(ApiPaths.Auth.LOGIN, new UserCredentials("login-user", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginRejectsUnknownUser() throws Exception {
        postJson(ApiPaths.Auth.LOGIN, new UserCredentials("no-such-user", "password-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshRotatesAndRevokesOldToken() throws Exception {
        AuthResponse first = readBody(register("rot-user", "password-123")
                .andExpect(status().isOk()), AuthResponse.class);

        AuthResponse rotated = readBody(postJson(ApiPaths.Auth.REFRESH,
                new RefreshTokenRequest(first.getRefreshToken()))
                .andExpect(status().isOk()), AuthResponse.class);

        assertThat(rotated.getRefreshToken()).isNotEqualTo(first.getRefreshToken());

        // старый refresh после ротации недействителен
        postJson(ApiPaths.Auth.REFRESH, new RefreshTokenRequest(first.getRefreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // новый работает
        postJson(ApiPaths.Auth.REFRESH, new RefreshTokenRequest(rotated.getRefreshToken()))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        AuthResponse session = readBody(register("logout-user", "password-123")
                .andExpect(status().isOk()), AuthResponse.class);

        postJson(ApiPaths.Auth.LOGOUT, new RefreshTokenRequest(session.getRefreshToken()))
                .andExpect(status().isNoContent());

        postJson(ApiPaths.Auth.REFRESH, new RefreshTokenRequest(session.getRefreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void validationFailureHasErrorCode() throws Exception {
        // username короче минимума из спеки (min=3)
        postJson(ApiPaths.Auth.REGISTER, new UserCredentials("ab", "password-123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private ResultActions register(String username, String password) throws Exception {
        return postJson(ApiPaths.Auth.REGISTER, new UserCredentials(username, password));
    }

    private ResultActions postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /** Читает тело выполненного запроса в DTO из common. */
    private <T> T readBody(ResultActions actions, Class<T> type) throws Exception {
        String json = actions.andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, type);
    }
}
