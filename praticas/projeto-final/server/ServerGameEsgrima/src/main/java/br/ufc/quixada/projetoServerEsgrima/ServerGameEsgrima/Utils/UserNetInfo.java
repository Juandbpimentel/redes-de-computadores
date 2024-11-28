package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserNetInfo {
    private User usuario;
    private ServerSocket serverSocketTcp;
    private Socket conexaoTcpParaComunicacaoSincrona;
    private Socket conexaoTcpParaBroadcast;

    private int portaUdpUsuario;

    private InetAddress ip;
    private UserStatus status;

    public UserNetInfo(int portaUdpUsuario,  InetAddress ip, UserStatus status) {
        this.portaUdpUsuario = portaUdpUsuario;
        this.ip = ip;
        this.status = status;
    }
}
