package br.com.polpabrasil.sincronizador.dao;

import br.com.polpabrasil.sincronizador.dto.ResponsavelCentroDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

/**
 * Acesso a tabela responsaveis_centro. SQL explicito, sem ORM.
 */
@Repository
public class ResponsavelCentroDao {

    private static final String SQL_UPSERT = """
            INSERT INTO responsaveis_centro
                (codcencus, descrcencus, analitico, ativo,
                 codusuresp, nomeusu, email, dtlimacesso,
                 lote_id, ativo_integracao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            ON DUPLICATE KEY UPDATE
                descrcencus      = VALUES(descrcencus),
                analitico        = VALUES(analitico),
                ativo            = VALUES(ativo),
                codusuresp       = VALUES(codusuresp),
                nomeusu          = VALUES(nomeusu),
                email            = VALUES(email),
                dtlimacesso      = VALUES(dtlimacesso),
                lote_id          = VALUES(lote_id),
                ativo_integracao = 1
            """;

    private static final String SQL_DESATIVA_AUSENTES = """
            UPDATE responsaveis_centro
               SET ativo_integracao = 0
             WHERE ativo_integracao = 1
               AND (lote_id IS NULL OR lote_id <> ?)
            """;

    private static final String SQL_CONTA_EXISTENTES = """
            SELECT COUNT(*) FROM responsaveis_centro WHERE codcencus IN (:codigos)
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public ResponsavelCentroDao(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    /**
     * Quantos dos codigos recebidos ja existem na tabela. Usado para separar
     * quantos serao insercao e quantos serao atualizacao.
     */
    public int contarExistentes(List<Integer> codigos) {
        if (codigos.isEmpty()) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource("codigos", codigos);
        Integer total = namedJdbc.queryForObject(SQL_CONTA_EXISTENTES, params, Integer.class);
        return total == null ? 0 : total;
    }

    /**
     * Insere ou atualiza todos os registros do lote numa unica ida ao banco.
     */
    public void upsertLote(List<ResponsavelCentroDTO> registros, long loteId) {
        jdbc.batchUpdate(SQL_UPSERT, registros, registros.size(), (ps, r) -> {
            ps.setInt(1, r.codcencus());
            ps.setString(2, r.descrcencus());
            ps.setString(3, r.analitico());
            ps.setString(4, r.ativo());
            ps.setInt(5, r.codusuresp());
            ps.setString(6, r.nomeusu());
            ps.setString(7, r.email());
            ps.setTimestamp(8, r.dtlimacesso() == null
                    ? null
                    : Timestamp.valueOf(r.dtlimacesso()));
            ps.setLong(9, loteId);
        });
    }

    /**
     * Marca como inativo tudo que estava ativo e nao veio neste lote.
     * Retorna quantas linhas foram desativadas.
     */
    public int desativarAusentes(long loteId) {
        return jdbc.update(SQL_DESATIVA_AUSENTES, loteId);
    }
}
