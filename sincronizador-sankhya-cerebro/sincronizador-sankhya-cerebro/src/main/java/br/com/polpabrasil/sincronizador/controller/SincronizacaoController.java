package br.com.polpabrasil.sincronizador.controller;

import br.com.polpabrasil.sincronizador.dto.SincronizacaoRequest;
import br.com.polpabrasil.sincronizador.dto.SincronizacaoResponse;
import br.com.polpabrasil.sincronizador.service.SincronizacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/responsaveis-centro")
public class SincronizacaoController {

    private final SincronizacaoService service;

    public SincronizacaoController(SincronizacaoService service) {
        this.service = service;
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<SincronizacaoResponse> sincronizar(
            @Valid @RequestBody SincronizacaoRequest request,
            HttpServletRequest http) {

        SincronizacaoResponse resposta = service.sincronizar(request, http.getRemoteAddr());
        return ResponseEntity.ok(resposta);
    }
}
