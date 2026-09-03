package br.com.polpabrasil.sincronizador.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload enviado pelo botao de acao do Sankhya.
 *
 * usuarioSankhya e opcional e serve apenas para auditoria: identifica quem
 * acionou o botao no ERP.
 */
public record SincronizacaoRequest(

        String usuarioSankhya,

        @NotEmpty(message = "a lista de registros nao pode estar vazia")
        @Valid
        List<ResponsavelCentroDTO> registros

) {
}
