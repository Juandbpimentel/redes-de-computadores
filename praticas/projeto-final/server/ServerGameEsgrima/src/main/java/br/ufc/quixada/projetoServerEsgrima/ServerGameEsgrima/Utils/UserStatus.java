package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils;

import lombok.Getter;

@Getter
public enum UserStatus {
    online( "online"),
    offline("offline"),
    in_game("in_game"),
    waiting_in_lobby("waiting_in_lobby"),
    waiting_for_challenge("waiting_for_challenge");

    private final String status;

    UserStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}