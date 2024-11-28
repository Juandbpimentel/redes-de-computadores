extends Node

# Configurações de rede
var isUDPConfigured:bool = false
var isServerConnected:bool = false
var isNetworkConfigured:bool = false

var udpCommunicationSocket:PacketPeerUDP = null

# 	Informações de rede do adversário
var portaSocketParDueloUDP:int = 0
var enderecoParDuelo:String = ""
var nicknameParDuelo:String = ""

# Informações e Sockets de rede para se comunicar com o servidor
var serverAdress:String = "127.0.0.1"
var serverTCPPort:int = 0
var serverCommunicationSocket:StreamPeerTCP = null
var serverBroadcastSocket:StreamPeerTCP = null

var secondsToWaitForServerResponse = 5
var secondsToWaitForAdversarioResponse = 5

# Threads
var configGlobalThreads:Array[Thread] = []
var genericThreads:Array[Thread] = []
var abortThreads = false

# Mensagens recebidas do servidor
var serverMessagesRecieved:Array[Dictionary] = []
# Mensagens recebidas do adversário
var adversarioMessagesRecieved:Array[Dictionary] = []

var userToken:Dictionary = {}
var isPlayer1:bool = false

var status:StatusEnum = StatusEnum.offline


enum StatusEnum {
	online,
	offline,
	in_game,
	waiting_in_lobby,
	waiting_for_challenge,
}

enum LoginScreenErrors {
	NONE,
	login_sucesso,
	registro_sucesso,
	timeout,
	falha_ao_enviar_pacote_tcp,
	falha_ao_enviar_pacote_udp,
	falha_ao_receber_pacote, 
	resposta_nula,
	erro_ao_estabelecer_conexao,
	erro_ao_esperar_pacote, 
	resposta_inesperada_do_servidor,
	erro_inexperado_ao_fazer_login_ou_registro,
	usuario_ja_existe,
	usuario_nao_encontrado,
	usuario_ja_esta_logado,
	senha_incorreta
}