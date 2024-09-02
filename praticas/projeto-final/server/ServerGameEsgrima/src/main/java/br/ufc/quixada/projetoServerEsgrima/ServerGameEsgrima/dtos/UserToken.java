package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

public record UserToken(
        String nickname,
        int victories,
        int defeats,
        int rank,
        String role
) {
}
