package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response;

public record AleatoryBattleInviteOperationResponse(
        String challengerIp,
        int challengerPort,
        String challengerNick,
        boolean isPlayerOne
) {
}
