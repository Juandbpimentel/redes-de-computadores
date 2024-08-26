extends Node

var recieverSocket:PacketPeerUDP = null
var serverCommunicationSocket:StreamPeerTCP = null
var socketParDoDuelo:int = 0
var enderecoParDuelo:String = ''

func fazerUpnp(porta:int):
	var upnp = UPNP.new()
	upnp.discover()
	if upnp.add_port_mapping(porta) != 0:
		print("houve um erro ao fazer a alocaç")

func conectarComServidor(endereco:String,porta:int):
	pass
	
func conectarComPar(endereco:String,porta:int):
	pass
