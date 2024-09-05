package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations;

public record BaseOperationRecord (
        String operation,
        String data,
        Boolean isEncrypted
){
}
