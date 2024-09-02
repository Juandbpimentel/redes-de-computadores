package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.controllers;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserRoles;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserStatus;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.*;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserNetInfo;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.GameService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.LobbyService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ControllerPrincipalServer implements CommandLineRunner {


    private final UserService userService;
    private final LobbyService lobbyService;
    private final GameService gameService;
    private final Map<String, UserNetInfo> usuariosDoSistema;

    @Value("${portaDoSocketUDP}")
    private Integer serverCommunicationsSocketPort;
    @Value("${ipDoServer}")
    private String serverIp;

    public ControllerPrincipalServer(UserService userService, LobbyService lobbyService, GameService gameService) {
        this.userService = userService;
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.usuariosDoSistema = new HashMap<>();
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("O server está ligado!");
        List<User> users = userService.getAllUsers();
        for (User user : users) {
            usuariosDoSistema.put(user.getNickname(), new UserNetInfo(user, null, null, null, null, 0, null, UserStatus.OFFLINE));
        }
        DatagramSocket socketServerUDP = new DatagramSocket(0, InetAddress.getByName(serverIp));
        new Thread(() -> handlePrincipalSocketCommunications(socketServerUDP.getLocalPort())).start();
        while (true) {
            byte[] receiveData = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socketServerUDP.receive(receivePacket);
            String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if (clientMessage.equals("closed_server")) {
                socketServerUDP.close();
                break;
            }
        }
        System.out.println("Server desligado");
    }

    public void handlePrincipalSocketCommunications(int portaDeFechamentoDoServidor) {
        ObjectMapper objectMapper = new ObjectMapper();
        DatagramSocket socketServerUDP;
        try {
            socketServerUDP = new DatagramSocket(serverCommunicationsSocketPort, InetAddress.getByName(serverIp));
        } catch (SocketException | UnknownHostException | NullPointerException e) {
            e.printStackTrace();
            return;
        }
        byte[] receiveData = new byte[2048];
        DatagramPacket receivePacket;


        while (true) {
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                socketServerUDP.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            InetAddress clientIPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            OperationRequest operationRequest = null;
            try {
                operationRequest = objectMapper.readValue(clientMessage, OperationRequest.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (operationRequest == null) {
                continue;
            }

            switch (operationRequest.operation()) {
                case "connect":
                    handleClientConnection(clientIPAddress, port, operationRequest);
                    break;
                case "close_server":
                    UserToken userToken = objectMapper.convertValue(operationRequest.data(), UserToken.class);
                    if (UserRoles.valueOf(userToken.role()) == UserRoles.ADMIN) {
                        try {
                            OperationRequest closeRequest = new OperationRequest("closed_server", null);
                            for (Map.Entry<String, UserNetInfo> entry : usuariosDoSistema.entrySet()) {
                                UserNetInfo userNetInfo = entry.getValue();
                                OutputStream saida = userNetInfo.getConexaoTcpParaBroadcast().getOutputStream();
                                saida.write(objectMapper.writeValueAsString(closeRequest).getBytes());
                                userNetInfo.getConexaoTcpParaBroadcast().close();
                                userNetInfo.getConexaoTcpParaComunicacaoSincrona().close();
                                userNetInfo.getServerSocketTcpParaBroadcast().close();
                                userNetInfo.getServerSocketTcpParaComunicacaoSincrona().close();
                            }
                            socketServerUDP.send(new DatagramPacket("closed_server".getBytes(), "closed_server".length(), InetAddress.getByName(serverIp), portaDeFechamentoDoServidor));
                            socketServerUDP.close();
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void handleClientConnection(InetAddress clientIPAddress, int clientUdpPort, OperationRequest operationRequest) {
        ObjectMapper objectMapper = new ObjectMapper();
        DatagramSocket socketServerUDP;
        try {
            socketServerUDP = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }


        ServerSocket socketParaComunicacaoDireta, socketParaBroadcast;
        Socket conexaoParaComunicacaoDireta = null;
        Socket conexaoParaBroadcast = null;
        UserLoginRequest user = objectMapper.convertValue(operationRequest.data(), UserLoginRequest.class);

        try {
            socketParaComunicacaoDireta = new ServerSocket(0);
            socketParaComunicacaoDireta.setReceiveBufferSize(2048);
            socketParaComunicacaoDireta.setSoTimeout(1000);
            socketParaBroadcast = new ServerSocket(0);
            socketParaBroadcast.setReceiveBufferSize(2048);
            socketParaBroadcast.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // avisa que abriu os sockets e manda pro client as portas
        ConnectionResponse connectionResponse = new ConnectionResponse("openned_sockets", socketParaComunicacaoDireta.getLocalPort(), socketParaBroadcast.getLocalPort());
        try {
            String responseString = objectMapper.writeValueAsString(connectionResponse);
            DatagramPacket responsePacket = new DatagramPacket(responseString.getBytes(), responseString.length(), clientIPAddress, clientUdpPort);
            socketServerUDP.send(responsePacket);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socketParaBroadcast.close();
                socketParaComunicacaoDireta.close();
            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
            return;
        }

        // Faz a aceite de conexão para comunicação direta e broadcast com o client
        for (int i = 0; i < 2; i++) {
            try {
                if (conexaoParaComunicacaoDireta == null) {
                    conexaoParaComunicacaoDireta = socketParaComunicacaoDireta.accept();
                }
                if (conexaoParaBroadcast == null) {
                    conexaoParaBroadcast = socketParaBroadcast.accept();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (i == 1) {
                    fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
                    return;
                }
            }
        }

        //faz o login do usuário
        User userLogado = userService.login(user.nickname(), user.password());
        if (userLogado == null) {
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        //confirma se todos os sockets estão conectados corretamente com o cliente e manda os dados para o client
        LoginResponse response = new LoginResponse("connected", userLogado.toUserToken());
        try {
            String responseString = objectMapper.writeValueAsString(response);
            if (conexaoParaComunicacaoDireta.getOutputStream() != null) {
                ObjectOutputStream saida = new ObjectOutputStream(conexaoParaComunicacaoDireta.getOutputStream());
                saida.writeObject(responseString);
                saida.flush();
            }
        } catch (Exception e) {
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }
        try {
            ObjectInputStream inputStream = new ObjectInputStream(conexaoParaComunicacaoDireta.getInputStream());
            String clientMessage = (String) inputStream.readObject();
            SingleMessageResponse clientResponse = objectMapper.readValue(clientMessage, SingleMessageResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        usuariosDoSistema.replace(userLogado.getNickname(), new UserNetInfo(userLogado, socketParaComunicacaoDireta, conexaoParaComunicacaoDireta, socketParaBroadcast, conexaoParaBroadcast, clientUdpPort, clientIPAddress, UserStatus.ONLINE));
        new Thread(() -> handleClient(userLogado.getNickname())).start();

    }

    public void handleClient(String nickname) {
        ObjectMapper objectMapper = new ObjectMapper();
        UserNetInfo usuarioCliente = usuariosDoSistema.get(nickname);
        int countTimeout = 0;
        try {
            usuarioCliente.getConexaoTcpParaBroadcast().setSoTimeout(120000);
            usuarioCliente.getConexaoTcpParaComunicacaoSincrona().setSoTimeout(120000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            while (true) {
                ObjectInputStream inputStream = new ObjectInputStream(usuarioCliente.getConexaoTcpParaComunicacaoSincrona().getInputStream());
                String clientMessage = (String) inputStream.readObject();
                OperationRequest operationRequest = objectMapper.readValue(clientMessage, OperationRequest.class);
                switch (operationRequest.operation()) {
                    case "msg":
                        SingleMessageResponse singleMessageResponse = objectMapper.convertValue(operationRequest.data(), SingleMessageResponse.class);
                        try {
                            for (UserNetInfo usuarioReceptor : usuariosDoSistema.values().stream().filter(
                                    user -> user.getStatus() == UserStatus.ONLINE &&
                                            !user.getUsuario().getNickname().equals(nickname)).toList()) {
                                ObjectOutputStream saida = new ObjectOutputStream(usuarioReceptor.getConexaoTcpParaBroadcast().getOutputStream());
                                saida.writeObject(clientMessage);
                                saida.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "logout":
                        UserNetInfo usuarioClienteNovo = usuariosDoSistema.get(nickname);
                        usuarioClienteNovo.getConexaoTcpParaBroadcast().close();
                        usuarioClienteNovo.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioClienteNovo.getServerSocketTcpParaBroadcast().close();
                        usuarioClienteNovo.getServerSocketTcpParaComunicacaoSincrona().close();
                        usuarioClienteNovo = new UserNetInfo(usuarioClienteNovo.getUsuario(), null, null, null, null, 0, null, UserStatus.OFFLINE);
                        usuariosDoSistema.replace(nickname, usuarioClienteNovo);
                        return;
                    default:
                        break;
                }
            }
        } catch (SocketTimeoutException e) {
            countTimeout++;
            if (countTimeout == 3) {
                UserNetInfo usuarioClienteNovo = usuariosDoSistema.get(nickname);
                try {
                    usuarioClienteNovo.getConexaoTcpParaBroadcast().close();
                    usuarioClienteNovo.getConexaoTcpParaComunicacaoSincrona().close();
                    usuarioClienteNovo.getServerSocketTcpParaBroadcast().close();
                    usuarioClienteNovo.getServerSocketTcpParaComunicacaoSincrona().close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
                usuarioClienteNovo = new UserNetInfo(usuarioClienteNovo.getUsuario(), null, null, null, null, 0, null, UserStatus.OFFLINE);
                usuariosDoSistema.replace(nickname, usuarioClienteNovo);
                return;
            }
        } catch (SocketException e) {
            if (e.getMessage().equals("Connection reset")) {
                UserNetInfo usuarioClienteNovo = usuariosDoSistema.get(nickname);
                try {
                    usuarioClienteNovo.getConexaoTcpParaBroadcast().close();
                    usuarioClienteNovo.getConexaoTcpParaComunicacaoSincrona().close();
                    usuarioClienteNovo.getServerSocketTcpParaBroadcast().close();
                    usuarioClienteNovo.getServerSocketTcpParaComunicacaoSincrona().close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
                usuarioClienteNovo = new UserNetInfo(usuarioClienteNovo.getUsuario(), null, null, null, null, 0, null, UserStatus.OFFLINE);
                usuariosDoSistema.replace(nickname, usuarioClienteNovo);
                return;
            }
            if (e.getMessage().equals("Socket closed")) {
                UserNetInfo usuarioClienteNovo = usuariosDoSistema.get(nickname);
                try {
                    usuarioClienteNovo.getConexaoTcpParaBroadcast().close();
                    usuarioClienteNovo.getConexaoTcpParaComunicacaoSincrona().close();
                    usuarioClienteNovo.getServerSocketTcpParaBroadcast().close();
                    usuarioClienteNovo.getServerSocketTcpParaComunicacaoSincrona().close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
                usuarioClienteNovo = new UserNetInfo(usuarioClienteNovo.getUsuario(), null, null, null, null, 0, null, UserStatus.OFFLINE);
                usuariosDoSistema.replace(nickname, usuarioClienteNovo);
                return;
            }

            if (e.getMessage().equals("Read timed out")) {
                UserNetInfo usuarioClienteNovo = usuariosDoSistema.get(nickname);
                try {
                    usuarioClienteNovo.getConexaoTcpParaBroadcast().close();
                    usuarioClienteNovo.getConexaoTcpParaComunicacaoSincrona().close();
                    usuarioClienteNovo.getServerSocketTcpParaBroadcast().close();
                    usuarioClienteNovo.getServerSocketTcpParaComunicacaoSincrona().close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
                usuarioClienteNovo = new UserNetInfo(usuarioClienteNovo.getUsuario(), null, null, null, null, 0, null, UserStatus.OFFLINE);
                usuariosDoSistema.replace(nickname, usuarioClienteNovo);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return;
        }
    }

    private void fechaConexoesQuandoDaErro(ServerSocket socketParaComunicacaoDireta, ServerSocket socketParaBroadcast, Socket conexaoParaComunicacaoDireta, Socket conexaoParaBroadcast) {
        if (conexaoParaComunicacaoDireta != null) {
            try {
                conexaoParaComunicacaoDireta.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        if (conexaoParaBroadcast != null) {
            try {
                conexaoParaBroadcast.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        try {
            socketParaBroadcast.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        try {
            socketParaComunicacaoDireta.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
