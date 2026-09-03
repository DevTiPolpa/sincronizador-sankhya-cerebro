package br.com.polpabrasil.sincronizador.service;

import br.com.polpabrasil.sincronizador.dao.ResponsavelCentroDao;
import br.com.polpabrasil.sincronizador.dao.SyncLoteDao;
import br.com.polpabrasil.sincronizador.dto.ResponsavelCentroDTO;
import br.com.polpabrasil.sincronizador.dto.SincronizacaoRequest;
import br.com.polpabrasil.sincronizador.dto.SincronizacaoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoService.class);

    private static final String ENTIDADE = "responsaveis_centro";

    private final ResponsavelCentroDao responsavelDao;
    private final SyncLoteDao loteDao;

    public SincronizacaoService(ResponsavelCentroDao responsavelDao, SyncLoteDao loteDao) {
        this.responsavelDao = responsavelDao;
        this.loteDao = loteDao;
    }

    /**
     * Fluxo:
     *   1. abre o lote
     *   2. conta quantos codigos ja existiam (para separar insert de update)
     *   3. faz o upsert em batch
     *   4. desativa quem nao veio nesta carga
     *   5. fecha o lote
     *
     * Tudo dentro de uma transacao: ou a carga inteira entra, ou nada entra.
     * Como sao poucos registros, isso e mais simples e mais seguro do que
     * tratar falha parcial.
     */
    @Transactional
    public SincronizacaoResponse sincronizar(SincronizacaoRequest request, String ipOrigem) {

        List<ResponsavelCentroDTO> registros = request.registros();
        int recebidos = registros.size();

        long loteId = loteDao.abrir(ENTIDADE, request.usuarioSankhya(), ipOrigem, recebidos);
        log.info("Lote {} aberto. Recebidos={} usuario={} origem={}",
                loteId, recebidos, request.usuarioSankhya(), ipOrigem);

        try {
            List<Integer> codigos = registros.stream()
                    .map(ResponsavelCentroDTO::codcencus)
                    .toList();

            int jaExistiam = responsavelDao.contarExistentes(codigos);
            int inseridos = recebidos - jaExistiam;

            responsavelDao.upsertLote(registros, loteId);
            int desativados = responsavelDao.desativarAusentes(loteId);

            loteDao.fechar(loteId, inseridos, jaExistiam, desativados, 0, "SUCESSO", null);

            log.info("Lote {} concluido. Inseridos={} atualizados={} desativados={}",
                    loteId, inseridos, jaExistiam, desativados);

            return new SincronizacaoResponse(
                    loteId, "SUCESSO", recebidos, inseridos, jaExistiam, desativados,
                    "Sincronizacao concluida com sucesso.");

        } catch (RuntimeException e) {
            // O rollback da transacao desfaz o upsert, mas o registro do lote
            // com status ERRO precisa sobreviver. Por isso ele e gravado em
            // transacao separada, no handler do controller.
            log.error("Falha no lote {}: {}", loteId, e.getMessage(), e);
            throw new SincronizacaoException(loteId, e.getMessage(), e);
        }
    }

    /**
     * Registra a falha do lote fora da transacao que sofreu rollback.
     */
    public void registrarFalha(long loteId, String mensagem) {
        try {
            loteDao.fechar(loteId, 0, 0, 0, 0, "ERRO", mensagem);
        } catch (RuntimeException e) {
            log.error("Nao foi possivel registrar a falha do lote {}", loteId, e);
        }
    }

    public static class SincronizacaoException extends RuntimeException {
        private final long loteId;

        public SincronizacaoException(long loteId, String mensagem, Throwable causa) {
            super(mensagem, causa);
            this.loteId = loteId;
        }

        public long getLoteId() {
            return loteId;
        }
    }
}
