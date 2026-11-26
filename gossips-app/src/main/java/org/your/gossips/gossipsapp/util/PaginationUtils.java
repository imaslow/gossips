package org.your.gossips.gossipsapp.util;

/**
 * Утилиты курсорной пагинации сообщений.
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    /**
     * Нормализует размер страницы: null заменяет значением по умолчанию,
     * значения вне диапазона [1, maxLimit] отклоняются {@link IllegalArgumentException}.
     *
     * @param limit       запрошенный размер страницы (может быть null)
     * @param defaultLimit значение по умолчанию при null
     * @param maxLimit    максимально допустимый размер страницы
     * @return нормализованный размер страницы
     */
    public static int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1 || limit > maxLimit) {
            throw new IllegalArgumentException("limit must be between 1 and " + maxLimit);
        }
        return limit;
    }
}
