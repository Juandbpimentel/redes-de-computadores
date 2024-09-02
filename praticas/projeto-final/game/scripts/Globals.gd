extends Node

var recieverSocket:PacketPeerUDP = null

var serverCommunicationSocket:StreamPeerTCP = null
var serverCommunicationSocketPort:int = 0

var serverBroadcastSocket:StreamPeerTCP = null
var serverBroadcastSocketPort:int = 0

var socketUDPParDoDuelo:int = 0
var enderecoParDuelo:String = ''

var enderecoDoServidorPadrao:String = 'localhost'
var portaDoServidorPadrao:int = 10001

func fazerUpnp(porta:int):
	var upnp = UPNP.new()
	upnp.discover()
	if upnp.add_port_mapping(porta) != 0:
		print("houve um erro ao fazer a alocação do socket no upnp")

func conectarComServidor(endereco:String,porta:int):
	pass
	
func conectarComPar(endereco:String,porta:int):
	pass
