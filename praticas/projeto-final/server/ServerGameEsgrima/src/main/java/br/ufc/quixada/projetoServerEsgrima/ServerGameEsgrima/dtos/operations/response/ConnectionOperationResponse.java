package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response;

public record ConnectionOperationResponse(
    int comunicationPort,
    int broadcastPort
){
}
