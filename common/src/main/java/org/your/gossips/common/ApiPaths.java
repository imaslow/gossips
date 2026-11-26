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

    public static final class Conversations {
        public static final String BASE = "/api/conversations";
        public static final String MESSAGES = BASE + "/{conversationId}/messages";
        public static final String READ = BASE + "/{conversationId}/read";

        private Conversations() {
        }
    }

    /**
     * WebSocket-эндпоинт realtime-доставки (только app-сервис).
     * Токен передаётся query-параметром: /ws?token=eyJ...
     */
    public static final class Realtime {
        public static final String WS = "/ws";

        private Realtime() {
        }
    }

    public static final class Keys {
        public static final String BASE = "/api/keys";
        public static final String ME = BASE + "/me";
        public static final String USER = BASE + "/{username}";
        public static final String VERIFY = USER + "/verify";

        private Keys() {
        }
    }

    public static final class Contacts {
        public static final String BASE = "/api/contacts";
        public static final String BY_USERNAME = BASE + "/{username}";

        private Contacts() {
        }
    }

    public static final class Profiles {
        public static final String BASE = "/api/profiles";
        public static final String ME = BASE + "/me";

        private Profiles() {
        }
    }
}
