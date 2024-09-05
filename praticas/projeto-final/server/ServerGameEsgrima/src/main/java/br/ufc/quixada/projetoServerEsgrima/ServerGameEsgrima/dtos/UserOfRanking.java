package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

public record UserOfRanking(
    String nickname,
    int victories,
    int defeats,
    int rank){

}
