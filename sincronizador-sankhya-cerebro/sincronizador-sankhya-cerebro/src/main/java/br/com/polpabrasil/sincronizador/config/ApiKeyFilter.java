package br.com.polpabrasil.sincronizador.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Autenticacao simples por chave de API.
 *
 * Só protege /api/**. O /actuator/health fica aberto de proposito, para
 * permitir teste de conectividade sem credencial.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    private static final String HEADER = "X-API-Key";

    private final String chaveEsperada;

    public ApiKeyFilter(@Value("${app.api-key}") String chaveEsperada) {
        this.chaveEsperada = chaveEsperada;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String recebida = request.getHeader(HEADER);

        if (recebida == null || !constantTimeEquals(recebida, chaveEsperada)) {
            log.warn("Requisicao rejeitada: chave invalida ou ausente. Origem={} URI={}",
                    request.getRemoteAddr(), request.getRequestURI());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"erro\":\"Chave de API invalida ou ausente\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Comparacao de tempo constante. Evita que a diferenca de tempo de resposta
     * entre chaves "quase certas" e "totalmente erradas" vaze informacao.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
