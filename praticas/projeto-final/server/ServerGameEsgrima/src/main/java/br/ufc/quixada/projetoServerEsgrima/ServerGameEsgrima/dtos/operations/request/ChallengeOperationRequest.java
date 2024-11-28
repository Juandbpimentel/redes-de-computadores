package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request;

public record ChallengeOperationRequest(
    String challenger,
    String challenged
) {
}
