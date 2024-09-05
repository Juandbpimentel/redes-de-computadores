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