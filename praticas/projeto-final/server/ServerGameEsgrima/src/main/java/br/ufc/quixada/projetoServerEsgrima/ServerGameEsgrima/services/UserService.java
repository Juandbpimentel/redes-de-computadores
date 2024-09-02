package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {
    private static BCryptPasswordEncoder passwordEcorder = new BCryptPasswordEncoder(10, new SecureRandom());

    public final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User usuarioNovo) {
        usuarioNovo.setPassword(passwordEcorder.encode(usuarioNovo.getPassword()));
        return userRepository.save(usuarioNovo);
    }

    public void updateUser(User usuario) {
        userRepository.save(usuario);
    }

    public void addVictory(User usuario){
        usuario.setVictories(usuario.getVictories() + 1);
        userRepository.save(usuario);
    }

    public void addDefeat(User usuario){
        usuario.setVictories(usuario.getDefeats() + 1);
        userRepository.save(usuario);
    }

    public User login(String nickname, String password) throws RuntimeException {
        User usuarioBanco = userRepository.findByNickname(nickname);
        if (usuarioBanco == null)
            throw new RuntimeException("Não existe um usuário com esse nickname");
        if (!passwordEcorder.matches(password, usuarioBanco.getPassword()))
            throw new RuntimeException("Sua senha não está certa");
        return usuarioBanco;
    }

    public List<User> getRanking(){
        List<User> ranking = userRepository.findAll();
        ranking.sort((a, b) -> b.getRanqueamento() - a.getRanqueamento());
        return ranking;
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }
}
