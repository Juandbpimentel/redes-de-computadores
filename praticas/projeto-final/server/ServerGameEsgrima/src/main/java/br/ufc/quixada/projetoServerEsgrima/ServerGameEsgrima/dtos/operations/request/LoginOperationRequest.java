package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request;

public record LoginOperationRequest(
        String nickname,
        String password
){}
