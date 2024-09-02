package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

public record LoginResponse(
        String msg,
        UserToken userToken
) {
}
