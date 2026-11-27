package org.your.gossips.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Сквозной идентификатор запроса: читает заголовок {@code X-Request-Id} от вызывающей
 * стороны (или генерирует UUID), кладёт его в MDC под ключом {@link #MDC_KEY} и возвращает
 * в ответе. Благодаря {@code logging.pattern.level} с {@code %X{requestId}} идентификатор
 * попадает в каждую строку лога запроса, а gateway дополнительно пробрасывает его
 * во внутренние сервисы — так цепочка «gateway → security/app» связывается одним id.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    /**
     * Текущий идентификатор запроса для прокидывания во внутренние вызовы.
     * Если фильтр ещё не успел отработать (фоновый поток и т.п.) — генерирует новый.
     */
    public static String current() {
        String requestId = MDC.get(MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
