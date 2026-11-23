package org.your.gossips.common;

/**
 * Пути API-эндпоинтов. Должны совпадать со спеками в src/main/resources/openapi/*.yml.
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    public static final class Auth {
        public static final String BASE = "/api/auth";
        public static final String REGISTER = BASE + "/register";
        public static final String LOGIN = BASE + "/login";
        public static final String REFRESH = BASE + "/refresh";
        public static final String LOGOUT = BASE + "/logout";

        private Auth() {
        }
    }

    public static final class Messages {
        public static final String BASE = "/api/messages";

        private Messages() {
        }
    }
}
