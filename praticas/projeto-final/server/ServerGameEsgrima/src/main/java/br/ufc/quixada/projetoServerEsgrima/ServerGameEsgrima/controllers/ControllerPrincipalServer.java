package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.controllers;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.OperationCode;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserRoles;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserStatus;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserOfRanking;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.UserToken;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.BaseOperation;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response.*;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request.ChallengeOperationRequest;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.MessageOperation;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request.LoginOperationRequest;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserNetInfo;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.GameService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.LobbyService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

@Component
public class ControllerPrincipalServer implements CommandLineRunner {


    private final UserService userService;
    private final LobbyService lobbyService;
    private final GameService gameService;
    private static Map<String, UserNetInfo> usuariosDoSistema;

    @Value("${portaDoSocketUDP}")
    private Integer serverCommunicationsSocketPort;
    @Value("${ipDoServer}")
    private String serverIp;
    private boolean isPlayerOne;

    public ControllerPrincipalServer(UserService userService, LobbyService lobbyService, GameService gameService) {
        this.userService = userService;
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.usuariosDoSistema = new HashMap<>();
    }

    //Main function
    @Override
    public void run(String... args) throws Exception {
        System.out.println("O server está ligado em " + serverIp + ":" + serverCommunicationsSocketPort);
        User admin = userService.userRepository.findByNickname("kinbofox");
        if (admin == null) {
            admin = new User("kinbofox", "admin", 0, 0, UserRoles.ADMIN);
            admin.setRole(UserRoles.ADMIN);
            userService.createUser(admin);
        }
        List<User> users = userService.getAllUsers();
        for (User user : users) {
            usuariosDoSistema.put(user.getNickname(), new UserNetInfo(user, null, null, null, 0, null, UserStatus.offline));
        }
        DatagramSocket socketServerUDP = new DatagramSocket(0, InetAddress.getByName(serverIp));
        new Thread(() -> {
            try {
                handlePrincipalSocketCommunications(socketServerUDP.getLocalPort());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        while (true) {
            DatagramPacket receivePacket = recebeMensagensPorUDP(socketServerUDP);
            if (receivePacket == null) {
                continue;
            }
            String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if (clientMessage.equals("closed_server")) {
                socketServerUDP.close();
                break;
            }
        }
        System.out.println("Server desligado");
    }



    // Handle server messages
    public void handlePrincipalSocketCommunications(int portaDeFechamentoDoServidor) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DatagramSocket socketServerUDP;
        try {
            socketServerUDP = new DatagramSocket(serverCommunicationsSocketPort, InetAddress.getByName(serverIp));
        } catch (SocketException | UnknownHostException | NullPointerException e) {
            e.printStackTrace();
            return;
        }


        while (true) {
            DatagramPacket receivePacket = recebeMensagensPorUDP(socketServerUDP);
            if (receivePacket == null) {
                continue;
            }
            InetAddress clientIPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String clientMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

            BaseOperation baseOperation = null;
            try {
                baseOperation = BaseOperation.readBase64(clientMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (baseOperation == null) {
                continue;
            }

            switch (baseOperation.getOperation().getCode()) {
                case "connect":
                    baseOperation.decryptData();
                    System.out.println(clientIPAddress + ":" + port + " às " + timestamp + ") Operation: " + baseOperation.getOperation() + " | Data: " + baseOperation.getDataAsString());
                    Thread thread = new Thread(() -> {
                        try {
                            handleClientConnection(clientIPAddress, port);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    thread.start();
                    break;
                case "close_server":
                    baseOperation.decryptData();
                    UserToken userToken = objectMapper.convertValue(baseOperation.getDataAsJsonNode(), UserToken.class);
                    if (UserRoles.valueOf(userToken.role()) == UserRoles.ADMIN) {
                        try {
                            BaseOperation closeRequest = new BaseOperation("closed_server",objectMapper.writeValueAsString(new MessageOperation("closed_server")),  false);
                            for (Map.Entry<String, UserNetInfo> entry : usuariosDoSistema.entrySet()) {
                                if (entry.getValue().getStatus() == UserStatus.offline) {
                                    continue;
                                }
                                UserNetInfo userNetInfo = entry.getValue();
                                OutputStream saida = userNetInfo.getConexaoTcpParaBroadcast().getOutputStream();
                                saida.write(objectMapper.writeValueAsString(closeRequest).getBytes());
                                userNetInfo.getConexaoTcpParaBroadcast().close();
                                userNetInfo.getConexaoTcpParaComunicacaoSincrona().close();
                                userNetInfo.getServerSocketTcp().close();
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



    // Handle Client Start Connection
    public void handleClientConnection(InetAddress clientIPAddress, int clientUdpPort) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DatagramSocket socketServerUDP;
        try {
            socketServerUDP = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // Settando os sockets de comunicação tcp com o client
        ServerSocket socketDoServer = null;
        Socket conexaoParaComunicacaoDireta = null;
        Socket conexaoParaBroadcast = null;
        try {
            socketDoServer = new ServerSocket(0);
            socketDoServer.setReceiveBufferSize(2048);
            socketDoServer.setSoTimeout(120000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Conexão com o cliente estabelecida: Socket do server tcp: " + socketDoServer.getLocalPort());


        // Criando o data para colocar no operation da requisição
        ConnectionOperationResponse connectionResponse = new ConnectionOperationResponse(socketDoServer.getLocalPort());
        String dataString = null;
        try {
            dataString = objectMapper.writeValueAsString(connectionResponse);
            if (dataString == null) return;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                socketDoServer.close();
            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
            return;
        }


        // Criando o pacote de resposta para o client e enviando
        BaseOperation responseBaseOperation = new BaseOperation("establish_connection", dataString, false);
        enviarBaseOperationPorUDP(socketServerUDP, clientIPAddress, clientUdpPort, responseBaseOperation, true);

        // Fazendo a aceite de conexão para comunicação direta e broadcast com o client
        for (int i = 0; i < 2; i++) {
            try {
                if (conexaoParaComunicacaoDireta == null) {
                    conexaoParaComunicacaoDireta = socketDoServer.accept();
                }
                if (conexaoParaBroadcast == null) {
                    conexaoParaBroadcast = socketDoServer.accept();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (i == 1) {
                    fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
                    return;
                }
            }
        }
        System.out.println("Conexão com o cliente estabelecida");

        // Recebendo pacote de login do cliente
        String clientMessage = recebeMensagensPorTCP(conexaoParaComunicacaoDireta);
        System.out.println("Mensagem do cliente: aaaaaaaaaaaaaaaaaaaaa | " + clientMessage);

        if ( clientMessage == null || clientMessage.isEmpty()){
            enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta,new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Erro inesperado ao fazer login ou registro")), false),false);
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        //convertendo json em string para objeto BaseOperation
        System.out.println("Mensagem do cliente: bbbbbbbb | " + clientMessage);
        BaseOperation requestBaseOperation = BaseOperation.readBase64(clientMessage);
        System.out.println("Mensagem do cliente: cccccccc | " + clientMessage);
        if (requestBaseOperation == null ||
                (!requestBaseOperation.getOperation().getCode().equals(OperationCode.login.getCode())
                        && !requestBaseOperation.getOperation().getCode().equals(OperationCode.register.getCode()))
        ) {
            enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta, new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Operação inválida")), false), false);
            enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta, new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Erro inesperado ao fazer login ou registro")), false), false);
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);

            return;
        }
        requestBaseOperation.decryptData();
        System.out.println("Objeto recebido: " + requestBaseOperation.stringify());

        LoginOperationRequest user = objectMapper.convertValue(requestBaseOperation.getDataAsObject(), LoginOperationRequest.class);


        //faz o login do usuário
        String errorMessage = null;
        User userLogadoOuRegistrado = null;
        Boolean isRegistro = requestBaseOperation.getOperation().getCode().equals(OperationCode.register.getCode());
        try {
            if (isRegistro)
                userLogadoOuRegistrado = userService.registerUser(user.nickname(), user.password());
            else
                userLogadoOuRegistrado = userService.login(user.nickname(), user.password());
            if (userLogadoOuRegistrado == null) {
                throw new RuntimeException("Erro inesperado ao fazer login");
            }
            if (usuariosDoSistema.get(userLogadoOuRegistrado.getNickname()).getStatus() == UserStatus.online) {
                throw new RuntimeException("Usuário já está logado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                errorMessage = e.getMessage();
            } else {
                errorMessage = "Erro inesperado ao fazer login";
            }
        }

        // trata erros no login do usuário
        if (userLogadoOuRegistrado == null || errorMessage != null) {
            try {
                if(isRegistro)
                    enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta,new BaseOperation(OperationCode.register_fail.getCode(), objectMapper.writeValueAsString(new MessageOperation(errorMessage)), false),false);
                else
                    enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta,new BaseOperation(OperationCode.login_fail.getCode(), objectMapper.writeValueAsString(new MessageOperation(errorMessage)), false),false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        System.out.println("teste 1");
        //confirma se todos os sockets estão conectados corretamente com o cliente e manda os dados para o client
        if (isRegistro)
            responseBaseOperation = new BaseOperation(OperationCode.register_success.getCode(), objectMapper.writeValueAsString(new LoginOperationResponse(OperationCode.register_success.getCode(), userLogadoOuRegistrado.toUserToken())),false);
        else
            responseBaseOperation = new BaseOperation(OperationCode.login_success.getCode(), objectMapper.writeValueAsString(new LoginOperationResponse(OperationCode.login_success.getCode(), userLogadoOuRegistrado.toUserToken())),false);
        try {
            if (conexaoParaComunicacaoDireta.getOutputStream() != null && conexaoParaBroadcast.getOutputStream() != null) {
                enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta, responseBaseOperation,true);
            }
        } catch (Exception e) {
            System.out.println("teste 1 erro");
            e.printStackTrace();
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }
        System.out.println("teste 2");
        //recebe a confirmação do cliente que o login foi bem sucedido
        clientMessage = recebeMensagensPorTCP(conexaoParaComunicacaoDireta);
        if (clientMessage == null || clientMessage.isEmpty()) {
            enviarBaseOperationPorTCP(conexaoParaComunicacaoDireta,new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Erro inesperado ao fazer login ou registro")), false),false);
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        requestBaseOperation = BaseOperation.readBase64(clientMessage);
        if (requestBaseOperation == null || !requestBaseOperation.getOperation().getCode().equals(OperationCode.ok.getCode())) {
            fechaConexoesQuandoDaErro(socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }
        System.out.println("Objeto recebido: " + requestBaseOperation.stringify());

        //adiciona o usuário ao sistema
        usuariosDoSistema.put(userLogadoOuRegistrado.getNickname(), new UserNetInfo(userLogadoOuRegistrado, socketDoServer, conexaoParaComunicacaoDireta, conexaoParaBroadcast, clientUdpPort, clientIPAddress, UserStatus.online));
        User finalUserLogadoOuRegistrado = userLogadoOuRegistrado;
        new Thread(() -> {
            try {
                handleClient(finalUserLogadoOuRegistrado.getNickname());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        if (isRegistro)
            System.out.println("Usuário " + userLogadoOuRegistrado.getNickname() + " registrado com sucesso");
        else
            System.out.println("Usuário " + userLogadoOuRegistrado.getNickname() + " logado com sucesso");
    }




    //Handle Connected Client
    public void handleClient(String nickname) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        UserNetInfo usuarioCliente = usuariosDoSistema.get(nickname);
        if (usuarioCliente == null) {
            System.out.println("Usuário não encontrado");
            return;
        }
        int countTimeout = 0;
        try {
            usuarioCliente.getConexaoTcpParaBroadcast().setSoTimeout(180000);
            usuarioCliente.getConexaoTcpParaComunicacaoSincrona().setSoTimeout(180000);
        } catch (Exception e) {
            System.out.println("Erro ao setar timeout");
            e.printStackTrace();
            return;
        }
        while (true) {
            try {
                String clientMessage = recebeMensagensPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona());
                BaseOperation baseOperation = BaseOperation.readBase64(clientMessage);
                if (baseOperation == null) {
                    enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Erro inesperado ao realizar operação")), false), false);
                    continue;
                }
                System.out.println("Objeto recebido: " + baseOperation.stringify());
                switch (baseOperation.getOperation().getCode()) {
                    case "challenge":
                        baseOperation.decryptData();
                        challengeHandler(objectMapper.convertValue(baseOperation.getDataAsObject(), ChallengeOperationRequest.class), usuarioCliente);
                        continue;
                    case "aleatory_battle":
                        aleatoryBattleHandler(usuarioCliente);
                        continue;
                    case "get_ranking":
                        List<User> ranking = userService.getRanking();
                        Map<Integer, UserOfRanking> rankingResponse = ranking.stream().map(user -> new UserOfRanking(user.getNickname(), user.getVictories(), user.getDefeats(), user.getRanqueamento())).collect(HashMap::new, (m, v) -> m.put(m.size(), v), Map::putAll);
                        enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.ranking.getCode(), objectMapper.writeValueAsString(new RankingResponse(rankingResponse, rankingResponse.get(nickname).rank())), false), true);
                        continue;
                    case "entered_in_battle":
                        usuarioCliente.setStatus(UserStatus.in_game);
                        continue;
                    case "finished_battle":
                        baseOperation.decryptData();
                        MessageOperation messageOperation = objectMapper.convertValue(baseOperation.getDataAsObject(), MessageOperation.class);
                        if (messageOperation.msg().equals("win")) {
                            usuarioCliente.getUsuario().won();
                        } else {
                            usuarioCliente.getUsuario().defeated();
                        }
                        userService.updateUser(usuarioCliente.getUsuario());
                        usuarioCliente.setStatus(UserStatus.online);
                        enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.update_user_token.getCode(), objectMapper.writeValueAsString(new LoginOperationResponse("Atualizados dados de batalhas",usuarioCliente.getUsuario().toUserToken())), false), false);
                        continue;
                    case "logout":
                        System.out.println("Usuário " + nickname + " desconectado");
                        usuarioCliente.getConexaoTcpParaBroadcast().close();
                        usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioCliente.getServerSocketTcp().close();
                        usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                        usuariosDoSistema.replace(nickname, usuarioCliente);
                        return;
                    default:
                        continue;
                }
            } catch (EOFException e){
                try {
                    usuarioCliente.getConexaoTcpParaBroadcast().close();
                    usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                    usuarioCliente.getServerSocketTcp().close();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
                usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                usuariosDoSistema.replace(nickname, usuarioCliente);
                System.out.println("Usuário " + nickname + " desconectado");
                return;
            } catch (SocketTimeoutException e) {
                countTimeout++;
                if (countTimeout == 3) {
                    try {
                        usuarioCliente.getConexaoTcpParaBroadcast().close();
                        usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioCliente.getServerSocketTcp().close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                    usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                    usuariosDoSistema.replace(nickname, usuarioCliente);
                    return;
                }
            } catch (SocketException e) {
                if (e.getMessage().equals("Connection reset")) {
                    try {
                        usuarioCliente.getConexaoTcpParaBroadcast().close();
                        usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioCliente.getServerSocketTcp().close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                    usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                    usuariosDoSistema.replace(nickname, usuarioCliente);
                    return;
                }
                if (e.getMessage().equals("Socket closed")) {
                    try {
                        usuarioCliente.getConexaoTcpParaBroadcast().close();
                        usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioCliente.getServerSocketTcp().close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                    usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                    usuariosDoSistema.replace(nickname, usuarioCliente);
                    return;
                }

                if (e.getMessage().equals("Read timed out")) {
                    try {
                        usuarioCliente.getConexaoTcpParaBroadcast().close();
                        usuarioCliente.getConexaoTcpParaComunicacaoSincrona().close();
                        usuarioCliente.getServerSocketTcp().close();
                    } catch (Exception ioException) {
                        ioException.printStackTrace();
                    }
                    usuarioCliente = new UserNetInfo(usuarioCliente.getUsuario(), null, null, null, 0, null, UserStatus.offline);
                    usuariosDoSistema.replace(nickname, usuarioCliente);
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public void challengeHandler(ChallengeOperationRequest challengeOperation, UserNetInfo usuarioCliente) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        User desafiado = userService.userRepository.findByNickname(challengeOperation.challenged());
        System.out.println("print 1");
        if (desafiado == null) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Usuário não encontrado")), false), false);
            return;
        }
        System.out.println("print 2");
        if (desafiado.getNickname().equals(usuarioCliente.getUsuario().getNickname())) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Você não pode se desafiar")), false), false);
            return;
        }
        UserNetInfo usuarioDesafiado = usuariosDoSistema.get(desafiado.getNickname());
        System.out.println("print 3");
        if (usuarioDesafiado == null) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Usuário desafiado não encontrado")), false), false);
            return;
        }
        System.out.println("print 4");
        if (usuarioDesafiado.getStatus() == UserStatus.offline) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Usuário desafiado está offline")), false), false);
            return;
        }
        System.out.println("print 5");
        if (usuarioDesafiado.getStatus() == UserStatus.in_game) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Usuário desafiado está batalhando")), false), false);
            return;
        }
        System.out.println("print 6");
        System.out.println("stadus desafiado"+usuarioDesafiado.getStatus().getStatus());
        if (!(usuarioDesafiado.getStatus() == UserStatus.waiting_for_challenge || usuarioDesafiado.getStatus() == UserStatus.online)) {
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Usuário desafiado está ocupado")), false), false);
            return;
        }
        System.out.println("print 7");
        System.out.println("mandou");
        boolean isPlayerOne = new Random().nextBoolean();
        enviarBaseOperationPorTCP(usuarioDesafiado.getConexaoTcpParaBroadcast(), new BaseOperation(OperationCode.challenge_invite.getCode(), objectMapper.writeValueAsString(new ChallengeInviteResponse(
                usuarioCliente.getIp().toString(), usuarioCliente.getPortaUdpUsuario(), usuarioCliente.getUsuario().getNickname(), isPlayerOne)), false), true);
        enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.challenge_invite.getCode(), objectMapper.writeValueAsString(new ChallengeInviteResponse(
                usuarioDesafiado.getIp().toString(), usuarioDesafiado.getPortaUdpUsuario(), usuarioDesafiado.getUsuario().getNickname(), !isPlayerOne)), false), true);
        usuarioCliente.setStatus(UserStatus.waiting_in_lobby);
        usuarioDesafiado.setStatus(UserStatus.waiting_in_lobby);
    }

    private void aleatoryBattleHandler(UserNetInfo usuarioCliente) throws JsonProcessingException {
        List<UserNetInfo> usuariosEsperandoPorBatalha = new ArrayList<>();
        for (Map.Entry<String, UserNetInfo> entry : usuariosDoSistema.entrySet()) {
            if (entry.getValue().getStatus() == UserStatus.waiting_for_challenge) {
                usuariosEsperandoPorBatalha.add(entry.getValue());
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        if (usuariosEsperandoPorBatalha.isEmpty()) {
            usuarioCliente.setStatus(UserStatus.waiting_for_challenge);
            usuariosDoSistema.replace(usuarioCliente.getUsuario().getNickname(), usuarioCliente);
            enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.aleatory_battle_queue.getCode(), objectMapper.writeValueAsString(new MessageOperation("Não há usuarios para batalhar agora, entrando na fila")), false), false);
            return;
        }
        UserNetInfo usuarioAdversario = null;
        do {
            usuarioAdversario = usuariosEsperandoPorBatalha.get(new Random().nextInt(usuariosEsperandoPorBatalha.size()));
        } while (usuarioAdversario.getUsuario().getNickname().equals(usuarioCliente.getUsuario().getNickname()));

        var isPlayerOne = new Random().nextBoolean();

        enviarBaseOperationPorTCP(usuarioCliente.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.aleatory_battle_invite.getCode(), objectMapper.writeValueAsString(new AleatoryBattleInviteOperationResponse(usuarioAdversario.getIp().toString(), usuarioAdversario.getPortaUdpUsuario(), usuarioAdversario.getUsuario().getNickname(), isPlayerOne)), false), true);
        if (usuarioAdversario.getStatus() == UserStatus.waiting_for_challenge) {
            enviarBaseOperationPorTCP(usuarioAdversario.getConexaoTcpParaBroadcast(), new BaseOperation(OperationCode.aleatory_battle_invite.getCode(), objectMapper.writeValueAsString(new AleatoryBattleInviteOperationResponse(usuarioCliente.getIp().toString(), usuarioCliente.getPortaUdpUsuario(), usuarioCliente.getUsuario().getNickname(), !isPlayerOne)), false), true);
        }else
            enviarBaseOperationPorTCP(usuarioAdversario.getConexaoTcpParaComunicacaoSincrona(), new BaseOperation(OperationCode.aleatory_battle_invite.getCode(), objectMapper.writeValueAsString(new AleatoryBattleInviteOperationResponse(usuarioCliente.getIp().toString(), usuarioCliente.getPortaUdpUsuario(), usuarioCliente.getUsuario().getNickname(), !isPlayerOne)), false), true);
        usuarioCliente.setStatus(UserStatus.waiting_in_lobby);
        usuarioAdversario.setStatus(UserStatus.waiting_in_lobby);
        usuariosDoSistema.replace(usuarioCliente.getUsuario().getNickname(), usuarioCliente);
        usuariosDoSistema.replace(usuarioAdversario.getUsuario().getNickname(), usuarioAdversario);
    }


    private void fechaConexoesQuandoDaErro(ServerSocket socketDoServer, Socket conexaoParaComunicacaoDireta, Socket conexaoParaBroadcast) {
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
            socketDoServer.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


    private String recebeMensagensPorTCP(Socket conexaoParaComunicacaoDireta) {
        String clientMessage = "";
        OutputStream saidaDeDadosProCliente = null;
        InputStream entradaDeDadosDoClient = null;
        try {
            // Criação dos streams de entrada e saída
            saidaDeDadosProCliente= conexaoParaComunicacaoDireta.getOutputStream();
            entradaDeDadosDoClient = conexaoParaComunicacaoDireta.getInputStream();

            // Debugging
            while (entradaDeDadosDoClient.available() == 0) {
                try {
                    Thread.sleep(1000);
                }catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            // Leitura do objeto enviado pelo cliente
            int size = entradaDeDadosDoClient.available();

            // Atribuição da mensagem do cliente
            clientMessage = new String(entradaDeDadosDoClient.readNBytes(size), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            // Trate a exceção conforme necessário
            return null;
        }
        return clientMessage;
    }

    private boolean enviarBaseOperationPorTCP(Socket conexaoParaComunicacaoDireta, BaseOperation mensagem, boolean encrypt) {
        OutputStream saidaDeDadosProCliente = null;
        InputStream entradaDeDadosDoClient = null;
        if (encrypt) {
            mensagem.encryptData();
        }
        try {
            // Criação dos streams de entrada e saída
            saidaDeDadosProCliente= conexaoParaComunicacaoDireta.getOutputStream();
            entradaDeDadosDoClient = conexaoParaComunicacaoDireta.getInputStream();
            saidaDeDadosProCliente.write(mensagem.stringifyToBase64());
            saidaDeDadosProCliente.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            // Trate a exceção conforme necessário
            return false;
        }
    }

    private boolean enviarBaseOperationPorUDP(DatagramSocket socketServerUDP, InetAddress clientIPAddress, int clientUdpPort, BaseOperation mensagem, boolean encrypt) {
        if (encrypt) {
            mensagem.encryptData();
        }
        try {
            DatagramPacket responsePacket = new DatagramPacket(mensagem.stringifyToBase64(), mensagem.stringifyToBase64().length, clientIPAddress, clientUdpPort);
            socketServerUDP.send(responsePacket);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private DatagramPacket recebeMensagensPorUDP(DatagramSocket socketServerUDP) {
        byte[] receiveData = new byte[2048];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socketServerUDP.receive(receivePacket);
            return receivePacket;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
