package br.com.polpabrasil.sincronizador.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Representa uma linha da view AD_VW_EXPORTA_CEREBRO_RESPONSAVEIS_CENTRO.
 * Os nomes dos campos batem com as colunas da view e com as colunas da
 * tabela responsaveis_centro no MariaDB.
 */
public record ResponsavelCentroDTO(

        @NotNull(message = "codcencus e obrigatorio")
        Integer codcencus,

        @NotBlank(message = "descrcencus e obrigatorio")
        @Size(max = 100, message = "descrcencus excede 100 caracteres")
        String descrcencus,

        @NotNull(message = "analitico e obrigatorio")
        @Pattern(regexp = "[SN]", message = "analitico deve ser S ou N")
        String analitico,

        @NotNull(message = "ativo e obrigatorio")
        @Pattern(regexp = "[SN]", message = "ativo deve ser S ou N")
        String ativo,

        @NotNull(message = "codusuresp e obrigatorio")
        Integer codusuresp,

        @NotBlank(message = "nomeusu e obrigatorio")
        @Size(max = 100, message = "nomeusu excede 100 caracteres")
        String nomeusu,

        @Size(max = 150, message = "email excede 150 caracteres")
        String email,

        /**
         * Nulo quando o usuario nao tem data limite de acesso.
         * Formato esperado: 2026-09-02T00:00:00
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dtlimacesso

) {
}
