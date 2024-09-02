package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

import com.fasterxml.jackson.databind.JsonNode;

public record OperationRequest(
        String operation,
        JsonNode data
) {
}
