package br.com.polpabrasil.sincronizador.dto;

/**
 * Retorno da sincronizacao. O botao de acao do Sankhya monta a mensagem
 * exibida ao usuario a partir destes campos.
 */
public record SincronizacaoResponse(

        Long loteId,
        String status,
        int recebidos,
        int inseridos,
        int atualizados,
        int desativados,
        String mensagem

) {
}
