package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserRoles;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserToken;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nickname;
    private String password;
    private int victories;
    private int defeats;
    private UserRoles role;

    public int getRanqueamento(){
        return victories - defeats;
    }

    public User(String nickname, String password, int victories, int defeats, UserRoles role) {
        this.nickname = nickname;
        this.password = password;
        this.victories = victories;
        this.defeats = defeats;
        this.role = UserRoles.USER;
    }

    public User(String nickname, String password){
        this.nickname = nickname;
        this.password = password;
        this.victories = 0;
        this.defeats = 0;
        this.role = UserRoles.USER;
    }

    public UserToken toUserToken(){
        return new UserToken(nickname, victories, defeats, getRanqueamento(), role.toString());
    }
}