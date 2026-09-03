package br.com.polpabrasil.sincronizador.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * Auditoria das cargas: abre um lote no inicio e fecha no fim, com as
 * contagens e o status.
 */
@Repository
public class SyncLoteDao {

    private static final String SQL_ABRE = """
            INSERT INTO sync_lote (entidade, origem, usuario_sankhya, ip_origem, qtd_recebida)
            VALUES (?, 'SANKHYA', ?, ?, ?)
            """;

    private static final String SQL_FECHA = """
            UPDATE sync_lote
               SET qtd_inserida   = ?,
                   qtd_atualizada = ?,
                   qtd_desativada = ?,
                   qtd_erro       = ?,
                   status         = ?,
                   mensagem       = ?,
                   dt_fim         = CURRENT_TIMESTAMP(3)
             WHERE id = ?
            """;

    private final JdbcTemplate jdbc;

    public SyncLoteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long abrir(String entidade, String usuarioSankhya, String ipOrigem, int qtdRecebida) {
        KeyHolder kh = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_ABRE, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entidade);
            ps.setString(2, usuarioSankhya);
            ps.setString(3, ipOrigem);
            ps.setInt(4, qtdRecebida);
            return ps;
        }, kh);

        Number id = kh.getKey();
        if (id == null) {
            throw new IllegalStateException("Nao foi possivel obter o id do lote gerado");
        }
        return id.longValue();
    }

    public void fechar(long loteId, int inseridos, int atualizados, int desativados,
                       int erros, String status, String mensagem) {
        jdbc.update(SQL_FECHA, inseridos, atualizados, desativados, erros, status, mensagem, loteId);
    }
}
