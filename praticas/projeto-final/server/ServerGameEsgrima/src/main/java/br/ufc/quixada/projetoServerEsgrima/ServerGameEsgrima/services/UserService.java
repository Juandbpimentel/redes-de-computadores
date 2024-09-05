package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {
    private static final BCryptPasswordEncoder passwordEncorder = new BCryptPasswordEncoder(10, new SecureRandom());

    public final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String nickname, String password) {
        User usuarioNovo = new User(nickname, password);
        usuarioNovo.setPassword(passwordEncorder.encode(usuarioNovo.getPassword()));
        if (userRepository.findByNickname(nickname) != null)
            throw new RuntimeException("Já existe um usuário com esse nickname");
        return userRepository.save(usuarioNovo);
    }

    public User createUser(User usuario) {
        usuario.setPassword(passwordEncorder.encode(usuario.getPassword()));
        if (userRepository.findByNickname(usuario.getNickname()) != null)
            throw new RuntimeException("Já existe um usuário com esse nickname");
        return userRepository.save(usuario);
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
        if (!passwordEncorder.matches(password, usuarioBanco.getPassword()))
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
