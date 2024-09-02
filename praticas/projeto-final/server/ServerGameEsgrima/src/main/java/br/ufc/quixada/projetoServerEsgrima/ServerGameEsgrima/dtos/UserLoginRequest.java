package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos;

public record UserLoginRequest(
        String nickname,
        String password
){}
