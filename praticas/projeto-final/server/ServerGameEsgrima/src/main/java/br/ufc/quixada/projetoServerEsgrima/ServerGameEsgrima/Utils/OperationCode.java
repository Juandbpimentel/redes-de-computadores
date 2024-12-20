package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils;

public enum OperationCode {
    msg("msg"),
    ok("ok"),
    connect("connect"),
    login("login"),
    establish_connection("establish_connection"),
    logout("logout"),
    register("register"),
    get_ranking("get_ranking"),
    challenge("challenge"),
    entered_in_battle("entered_in_battle"),
    finished_battle("finished_battle"),
    update_user_token("update_user_token"),
    challenge_invite("challenge_invite"),
    aleatory_battle("aleatory_battle"),
    aleatory_battle_invite("aleatory_battle_invite"),
    aleatory_battle_queue("aleatory_battle_queue"),
    connection_success("connection_success"),
    login_success("login_success"),
    establish_connection_success("establish_connection_success"),
    logout_success("logout_success"),
    register_success("register_success"),
    ranking("ranking"),
    connection_fail("connection_fail"),
    login_fail("login_fail"),
    logout_fail("logout_fail"),
    register_fail("register_fail"),
    error("error");

    private final String code;

    OperationCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
