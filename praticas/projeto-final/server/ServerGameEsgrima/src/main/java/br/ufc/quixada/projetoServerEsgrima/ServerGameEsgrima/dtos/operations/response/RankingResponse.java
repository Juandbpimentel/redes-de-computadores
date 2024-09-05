package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserOfRanking;

import java.util.Map;

public record RankingResponse(
    Map<Integer, UserOfRanking> ranking,
    int myRanking) {
}