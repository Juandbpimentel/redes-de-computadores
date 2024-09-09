package br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.controllers;

import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.OperationCode;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserRoles;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserStatus;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.BaseOperation;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response.ConnectionOperationResponse;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.response.LoginOperationResponse;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.MessageOperation;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.dtos.operations.request.LoginOperationRequest;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.models.User;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.Utils.UserNetInfo;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.EncryptionService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.GameService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.LobbyService;
import br.ufc.quixada.projetoServerEsgrima.ServerGameEsgrima.services.UserService;
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
        System.out.println("O server está ligado em " + serverIp + ":" + serverCommunicationsSocketPort);
        User admin = userService.userRepository.findByNickname("kinbofox");
        if (admin == null) {
            admin = new User("kinbofox", "admin", 0, 0, UserRoles.ADMIN);
            admin.setRole(UserRoles.ADMIN);
            userService.createUser(admin);
        }
        List<User> users = userService.getAllUsers();
        for (User user : users) {
            usuariosDoSistema.put(user.getNickname(), new UserNetInfo(user, null, null, null, null, 0, null, UserStatus.OFFLINE));
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

    public void handlePrincipalSocketCommunications(int portaDeFechamentoDoServidor) throws Exception {
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

            if (baseOperation.getOperation().equals("connect")) {
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

//            switch (baseOperation.operation()) {
//                case "connect":
//                    break;
//                case "close_server":
//                    UserToken userToken = objectMapper.convertValue(operationRequest.data(), UserToken.class);
//                    if (UserRoles.valueOf(userToken.role()) == UserRoles.ADMIN) {
//                        try {
//                            OperationRequest closeRequest = new OperationRequest("closed_server", null);
//                            for (Map.Entry<String, UserNetInfo> entry : usuariosDoSistema.entrySet()) {
//                                UserNetInfo userNetInfo = entry.getValue();
//                                OutputStream saida = userNetInfo.getConexaoTcpParaBroadcast().getOutputStream();
//                                saida.write(objectMapper.writeValueAsString(closeRequest).getBytes());
//                                userNetInfo.getConexaoTcpParaBroadcast().close();
//                                userNetInfo.getConexaoTcpParaComunicacaoSincrona().close();
//                                userNetInfo.getServerSocketTcpParaBroadcast().close();
//                                userNetInfo.getServerSocketTcpParaComunicacaoSincrona().close();
//                            }
//                            socketServerUDP.send(new DatagramPacket("closed_server".getBytes(), "closed_server".length(), InetAddress.getByName(serverIp), portaDeFechamentoDoServidor));
//                            socketServerUDP.close();
//                            return;
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    break;
//                default:
//                    break;
//            }
            }
        }
    }

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
        ServerSocket socketParaComunicacaoDireta = null;
        ServerSocket socketParaBroadcast = null;
        Socket conexaoParaComunicacaoDireta = null;
        Socket conexaoParaBroadcast = null;
        try {
            socketParaComunicacaoDireta = new ServerSocket(0);
            socketParaComunicacaoDireta.setReceiveBufferSize(2048);
            socketParaComunicacaoDireta.setSoTimeout(120000);
            socketParaBroadcast = new ServerSocket(0);
            socketParaBroadcast.setReceiveBufferSize(2048);
            socketParaBroadcast.setSoTimeout(120000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Conexão com o cliente estabelecida: Socket de comunicação direta: " + socketParaComunicacaoDireta.getLocalPort() + " | Socket de broadcast: " + socketParaBroadcast.getLocalPort());


        // Criando o data para colocar no operation da requisição
        ConnectionOperationResponse connectionResponse = new ConnectionOperationResponse(socketParaComunicacaoDireta.getLocalPort(), socketParaBroadcast.getLocalPort());
        String dataString = null;
        try {
            dataString = objectMapper.writeValueAsString(connectionResponse);
            if (dataString == null) return;
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


        // Criando o pacote de resposta para o client e enviando
        BaseOperation responseBaseOperation = new BaseOperation("establish_connection", dataString, false);
        responseBaseOperation.encryptData();
        DatagramPacket responsePacket = new DatagramPacket(responseBaseOperation.stringifyToBase64(), responseBaseOperation.stringifyToBase64().length, clientIPAddress, clientUdpPort);
        System.out.println("Enviando pacote de resposta para o client, dados do pacote: " + clientIPAddress + ":" + clientUdpPort + " | " + responseBaseOperation.stringify());
        try {
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

        // Fazendo a aceite de conexão para comunicação direta e broadcast com o client
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

        System.out.println("Conexão com o cliente estabelecida");
        // Recebendo pacote de login do cliente
        String clientMessage = null;
        OutputStream saidaDeDadosProCliente = null;
        InputStream entradaDeDadosDoClient = null;
        try {
            // Criação dos streams de entrada e saída
            saidaDeDadosProCliente= conexaoParaComunicacaoDireta.getOutputStream();
            entradaDeDadosDoClient = conexaoParaComunicacaoDireta.getInputStream();

            // Debugging
            System.out.println("Streams criados com sucesso");
            while (entradaDeDadosDoClient.available() == 0) {
                Thread.sleep(1000);
            }
            // Leitura do objeto enviado pelo cliente
            int size = entradaDeDadosDoClient.available();
            System.out.println("Tamanho do objeto recebido: " + size);
            String teste = new String(entradaDeDadosDoClient.readNBytes(size), StandardCharsets.UTF_8);

            // Atribuição da mensagem do cliente
            clientMessage = teste;
            System.out.println("Mensagem do cliente: " + clientMessage);
        } catch (IOException e) {
            e.printStackTrace();
            // Trate a exceção conforme necessário
        }

        //convertendo json em string para objeto BaseOperation

        BaseOperation requestBaseOperation = BaseOperation.readBase64(clientMessage);
        if (requestBaseOperation == null ||
                (!requestBaseOperation.getOperation().equals(OperationCode.login.getCode())
                        && !requestBaseOperation.getOperation().equals(OperationCode.register.getCode()))
        ) {

            saidaDeDadosProCliente.write(new BaseOperation(OperationCode.error.getCode(), objectMapper.writeValueAsString(new MessageOperation("Erro inexperado ao fazer ou registro")), false).stringifyToBase64());
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);

            return;
        }
        requestBaseOperation.decryptData();
        System.out.println("Objeto recebido: " + requestBaseOperation.stringify());

        LoginOperationRequest user = objectMapper.convertValue(requestBaseOperation.getDataAsObject(), LoginOperationRequest.class);


        //faz o login do usuário
        String errorMessage = null;
        User userLogadoOuRegistrado = null;
        Boolean isRegistro = requestBaseOperation.getOperation().equals(OperationCode.register.getCode());
        try {
            if (isRegistro)
                userLogadoOuRegistrado = userService.registerUser(user.nickname(), user.password());
            else
                userLogadoOuRegistrado = userService.login(user.nickname(), user.password());
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                errorMessage = e.getMessage();
            } else {
                errorMessage = "Erro inexperado ao fazer login";
            }
        }

        // trata erros no login do usuário
        if (userLogadoOuRegistrado == null) {
            try {
                if(isRegistro)
                    saidaDeDadosProCliente.write(new BaseOperation(OperationCode.register_fail.getCode(), objectMapper.writeValueAsString(new MessageOperation(errorMessage)), false).stringifyToBase64());
                else
                    saidaDeDadosProCliente.write(new BaseOperation(OperationCode.login_fail.getCode(), objectMapper.writeValueAsString(new MessageOperation(errorMessage)), false).stringifyToBase64());
            } catch (Exception e) {
                e.printStackTrace();
            }
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        //confirma se todos os sockets estão conectados corretamente com o cliente e manda os dados para o client
        if (isRegistro)
            responseBaseOperation = new BaseOperation(OperationCode.register_success.getCode(), objectMapper.writeValueAsString(new LoginOperationResponse(OperationCode.register_success.getCode(), userLogadoOuRegistrado.toUserToken())),false);
        else
            responseBaseOperation = new BaseOperation(OperationCode.login_success.getCode(), objectMapper.writeValueAsString(new LoginOperationResponse(OperationCode.login_success.getCode(), userLogadoOuRegistrado.toUserToken())),false);
        try {
            responseBaseOperation.encryptData();
            if (conexaoParaComunicacaoDireta.getOutputStream() != null && conexaoParaBroadcast.getOutputStream() != null) {
                DataOutputStream saida = new DataOutputStream(conexaoParaComunicacaoDireta.getOutputStream());
                saida.write(responseBaseOperation.stringifyToBase64());
                saida.flush();
            }
        } catch (Exception e) {
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        //recebe a confirmação do cliente que o login foi bem sucedido
        try {
            // Debugging
            while (entradaDeDadosDoClient.available() == 0) {
                Thread.sleep(1000);
            }
            // Leitura do objeto enviado pelo cliente
            int size = entradaDeDadosDoClient.available();
            System.out.println("Tamanho do objeto recebido: " + size);
            String teste = new String(entradaDeDadosDoClient.readNBytes(size), StandardCharsets.UTF_8);

            // Atribuição da mensagem do cliente
            clientMessage = teste;
            System.out.println("Mensagem do cliente: " + clientMessage);
            requestBaseOperation = BaseOperation.readBase64(clientMessage);
            System.out.println("Objeto recebido: " + requestBaseOperation.stringify());
            if (requestBaseOperation == null || !requestBaseOperation.getOperation().equals(OperationCode.ok.getCode())) {
                fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            fechaConexoesQuandoDaErro(socketParaComunicacaoDireta, socketParaBroadcast, conexaoParaComunicacaoDireta, conexaoParaBroadcast);
            return;
        }

        usuariosDoSistema.put(userLogadoOuRegistrado.getNickname(), new UserNetInfo(userLogadoOuRegistrado, socketParaComunicacaoDireta, conexaoParaComunicacaoDireta, socketParaBroadcast, conexaoParaBroadcast, clientUdpPort, clientIPAddress, UserStatus.ONLINE));
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

    public void handleClient(String nickname) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        UserNetInfo usuarioCliente = usuariosDoSistema.get(nickname);
        if (usuarioCliente == null) {
            System.out.println("Usuário não encontrado");
            return;
        }
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
                BaseOperation baseOperation = objectMapper.readValue(clientMessage, BaseOperation.class);
                switch (baseOperation.getOperation()) {
                    case "msg":
                        MessageOperation singleMessageResponse = objectMapper.convertValue(baseOperation.getData(), MessageOperation.class);
                        try {
                            for (UserNetInfo usuarioReceptor : usuariosDoSistema.values().stream().filter(user -> user.getStatus() == UserStatus.ONLINE && !user.getUsuario().getNickname().equals(nickname)).toList()) {
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
