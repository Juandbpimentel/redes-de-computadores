package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response;

public record ChallengeInviteResponse(
    String challengerIp,
    int challengerPort,
    String challengerNick,
    boolean isPlayerOne
) {
}
