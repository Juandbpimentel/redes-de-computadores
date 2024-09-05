package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserToken;

public record LoginOperationResponse(
        String msg,
        UserToken userToken
) {
}
