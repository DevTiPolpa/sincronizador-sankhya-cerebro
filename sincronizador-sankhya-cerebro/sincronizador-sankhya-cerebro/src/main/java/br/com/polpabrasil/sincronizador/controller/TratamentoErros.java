package br.com.polpabrasil.sincronizador.controller;

import br.com.polpabrasil.sincronizador.dto.SincronizacaoResponse;
import br.com.polpabrasil.sincronizador.service.SincronizacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Converte excecoes em respostas JSON no mesmo formato do sucesso, para o
 * botao de acao do Sankhya conseguir ler a mensagem sem tratamento especial.
 */
@RestControllerAdvice
public class TratamentoErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoErros.class);

    private final SincronizacaoService service;

    public TratamentoErros(SincronizacaoService service) {
        this.service = service;
    }

    /**
     * Payload malformado: campos faltando, fora do tamanho, valor invalido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SincronizacaoResponse> validacao(MethodArgumentNotValidException e) {

        String detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .distinct()
                .limit(10)
                .collect(Collectors.joining("; "));

        log.warn("Payload rejeitado na validacao: {}", detalhes);

        return ResponseEntity.badRequest().body(new SincronizacaoResponse(
                null, "ERRO", 0, 0, 0, 0, "Dados invalidos - " + detalhes));
    }

    /**
     * Falha durante a gravacao. O lote ja foi aberto, entao marcamos ele como
     * ERRO numa transacao nova (a original sofreu rollback).
     */
    @ExceptionHandler(SincronizacaoService.SincronizacaoException.class)
    public ResponseEntity<SincronizacaoResponse> sincronizacao(
            SincronizacaoService.SincronizacaoException e) {

        service.registrarFalha(e.getLoteId(), e.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SincronizacaoResponse(
                        e.getLoteId(), "ERRO", 0, 0, 0, 0,
                        "Falha ao gravar: " + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SincronizacaoResponse> generico(Exception e) {
        log.error("Erro nao tratado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SincronizacaoResponse(
                        null, "ERRO", 0, 0, 0, 0, "Erro interno: " + e.getMessage()));
    }
}
