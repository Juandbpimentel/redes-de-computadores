extends Node

# Configurações de rede
var isNetworkConfigured:bool = false

var udpCommunicationSocket:PacketPeerUDP = null

# 	Informações de rede do adversário
var portaSocketParDueloUDP:int = 0
var enderecoParDuelo:String = ""

# Informações e Sockets de rede para se comunicar com o servidor
var serverCommunicationSocket:StreamPeerTCP = null
var serverCommunicationSocketPort:int = 0
var serverBroadcastSocket:StreamPeerTCP = null
var serverBroadcastSocketPort:int = 0
var serverAdress:String = "127.0.0.1"

var secondsToWaitForServerResponse = 5
var secondsToWaitForAdversarioResponse = 5

# Threads
var configGlobalThreads:Array[Thread] = []
var genericThreads:Array[Thread] = []

# Mensagens recebidas do servidor
var serverMessagesRecieved:Array[Dictionary] = []
# Mensagens recebidas do adversário
var adversarioMessagesRecieved:Array[Dictionary] = []


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
	senha_incorreta
}