package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

public record ConnectionResponse (
    String msg,
    int comunicationPort,
    int broadcastPort
){

}
