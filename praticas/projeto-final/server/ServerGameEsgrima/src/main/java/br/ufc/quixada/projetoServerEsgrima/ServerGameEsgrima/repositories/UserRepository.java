package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.repositories;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
    User findByNickname(String nickname);
}