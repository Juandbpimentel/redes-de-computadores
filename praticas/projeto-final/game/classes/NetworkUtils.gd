class_name NetworkUtils extends Node

enum LoginScreenErrors {
	OK,
	timeout,
	falha_ao_enviar_pacote_tcp,
	falha_ao_enviar_pacote_udp,
	falha_ao_receber_pacote, 
	resposta_nula,
	erro_ao_estabelecer_conexao,
	erro_ao_esperar_pacote, 
	resposta_inesperada_do_servidor,
	erro_inexperado_ao_fazer_ou_registro,
	usuario_ja_existe,
	usuario_nao_encontrado,
	senha_incorreta
}


static func sendMessageWithUDPCommunicationSocket(message:Variant, address:String, port:int, encrypt:bool) -> Error:
	var finalMessage:OperationRequestBody = OperationRequestBody.new(message)
	
	if encrypt && !finalMessage.get_is_encrypted():
		finalMessage.encrypt_data()
	if !encrypt && finalMessage.get_is_encrypted():
		finalMessage.decrypt_data()

	var err = Globals.udpCommunicationSocket.set_dest_address(address, port)
	if err != OK:
		push_error(str(err))
		return FAILED

	err = Globals.udpCommunicationSocket.put_packet(Marshalls.raw_to_base64(finalMessage.stringify().to_utf8_buffer()).to_utf8_buffer())
	if err != OK:
		push_error(str(err))
		return FAILED
	return OK

static func killSocketWaitOnTimeout(timeout:int) -> void:
	var count = 0
	while Globals.udpCommunicationSocket.get_available_packet_count() == 0:
		OS.delay_msec(1000)
		count += 1
		if count >= timeout:
			var newSocket = PacketPeerUDP.new()
			newSocket.bind(0)
			newSocket.set_dest_address(IP.resolve_hostname(str(OS.get_environment("HOSTNAME")),1), Globals.udpCommunicationSocket.get_local_port())
			newSocket.put_packet(Marshalls.raw_to_base64("kill".to_utf8_buffer()).to_utf8_buffer())
			newSocket.close()
			break

static func recieveMessageWithUDPCommunicationSocket() -> Variant:
	var thread = Thread.new()
	thread.start(killSocketWaitOnTimeout.bind(5))
	if Globals.udpCommunicationSocket.wait() == OK:
		var response:PackedByteArray = Globals.udpCommunicationSocket.get_packet()
		if response != null:
			var responseString = EncryptAndDecodeUtils.decode_base64(response.get_string_from_utf8()).get_string_from_utf8()
			if responseString == "kill":
				return Globals.LoginScreenErrors.timeout
			return OperationRequestBody.new(responseString)
		else:
			return Globals.LoginScreenErrors.resposta_nula
	else:
		return Globals.LoginScreenErrors.erro_ao_esperar_pacote


static func sendMessageWithTCPCommunicationSocket(message:Variant) -> Error:
	if Globals.serverCommunicationSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
		while Globals.serverCommunicationSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
			Globals.serverCommunicationSocket.poll()
			pass
	if Globals.serverCommunicationSocket.get_status() != StreamPeerTCP.STATUS_CONNECTED:
		push_error("socket não conectado")
		return FAILED
	var finalMessage:OperationRequestBody = OperationRequestBody.new(message)
	var err = Globals.serverCommunicationSocket.put_data(Marshalls.raw_to_base64(finalMessage.stringify().to_utf8_buffer()).to_utf8_buffer())
	if err != OK:
		push_error(str(err))
		return FAILED
	print("mensagem enviada: ", message)
	return OK


static func recieveMessageWithTCPCommunicationSocket(bufferSize:int) -> OperationRequestBody:
	if Globals.serverCommunicationSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
		while Globals.serverCommunicationSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
			Globals.serverCommunicationSocket.poll()
			pass
	
	var count = 0
	while Globals.serverCommunicationSocket.get_available_bytes() == 0:
		OS.delay_msec(1000)
		count += 1
		if count >= Globals.secondsToWaitForServerResponse:
			push_error("timeout")
			return null
	if Globals.serverCommunicationSocket.get_status() == StreamPeerTCP.STATUS_CONNECTED:
		var response: Array = Globals.serverCommunicationSocket.get_partial_data(bufferSize)
		if response != null:
			return OperationRequestBody.new(EncryptAndDecodeUtils.decode_base64(response[1].get_string_from_utf8()).get_string_from_utf8())
		else:
			push_error("resposta nula")
			return null
	else:
		push_error("erro ao esperar pacote")
		return null

static func sendMessageWithTCPBroadcastSocket(message:Variant) -> Error:
	if Globals.serverBroadcastSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
		while Globals.serverBroadcastSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
			Globals.serverBroadcastSocket.poll()
			pass
	if Globals.serverBroadcastSocket.get_status() != StreamPeerTCP.STATUS_CONNECTED:
		push_error("socket não conectado")
		return FAILED
	var finalMessage:OperationRequestBody = OperationRequestBody.new(message)
	var err = Globals.serverBroadcastSocket.put_data(Marshalls.raw_to_base64(finalMessage.stringify().to_utf8_buffer()).to_utf8_buffer())
	if err != OK:
		push_error(str(err))
		return FAILED
	return OK


static func recieveMessageWithTCPBroadcastSocket(bufferSize:int) -> OperationRequestBody:
	if Globals.serverBroadcastSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
		while Globals.serverBroadcastSocket.get_status() == StreamPeerTCP.STATUS_CONNECTING:
			Globals.serverBroadcastSocket.poll()
			pass
	
	var count = 0
	while Globals.serverBroadcastSocket != null && Globals.serverBroadcastSocket.get_available_bytes() == 0:
		OS.delay_msec(1000)
		count += 1
		if count >= Globals.secondsToWaitForServerResponse:
			push_error("timeout")
			return null
	
	if Globals.serverBroadcastSocket != null && Globals.serverBroadcastSocket.get_status() == StreamPeerTCP.STATUS_CONNECTED:
		var response: Array = Globals.serverBroadcastSocket.get_partial_data(bufferSize)
		if response != null:
			return OperationRequestBody.new(EncryptAndDecodeUtils.decode_base64(response[1].get_string_from_utf8()).get_string_from_utf8())
		else:
			push_error("resposta nula")
			return null
	else:
		push_error("erro ao esperar pacote")
		return null









static func connectToServer() -> Error:
	var err = Globals.serverCommunicationSocket.connect_to_host(Globals.serverAdress, Globals.serverTCPPort)
	if err != OK:
		push_error(str(err))
		return FAILED

	err = Globals.serverBroadcastSocket.connect_to_host(Globals.serverAdress, Globals.serverTCPPort)
	if err != OK:
		push_error(str(err))
		return FAILED
	return OK


static func defineServerPortsAndAdress(adress:String, serverPort:int):
	Globals.serverAdress = adress
	Globals.serverTCPPort = serverPort

static func alocRecieverSocket():
	Globals.udpCommunicationSocket = PacketPeerUDP.new()
	Globals.udpCommunicationSocket.bind(EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao)
	
	if Globals.udpCommunicationSocket.get_local_port() != EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao:
		if Globals.udpCommunicationSocket.get_local_port() == 0:
			Globals.udpCommunicationSocket.close()
			Globals.udpCommunicationSocket = PacketPeerUDP.new()
			Globals.udpCommunicationSocket.bind(0)
		EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao = Globals.udpCommunicationSocket.get_local_port()
		
	
	UPNPJava.addPortMapping(EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao, EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao, "udp")
	Globals.isUDPConfigured = true

static func alocServerSockets():
	Globals.serverCommunicationSocket = StreamPeerTCP.new()
	Globals.serverCommunicationSocket.bind(0)
	Globals.serverBroadcastSocket = StreamPeerTCP.new()
	Globals.serverBroadcastSocket.bind(0)
	Globals.isServerConnected = true

static func alocAllSockets():
	if Globals.isNetworkConfigured:
		return
	
	if !Globals.isUDPConfigured:
		alocRecieverSocket()
	if !Globals.isServerConnected:
		alocServerSockets()

	if !Globals.isUDPConfigured:
		Globals.isNetworkConfigured = false
		closeConnectionToServer()
		return
	elif !Globals.isServerConnected:
		Globals.isNetworkConfigured = false
		closeUDP()
		return
	
	Globals.isNetworkConfigured = true




static func closeConnectionToServer():
	if !Globals.isServerConnected:
		return
	Globals.serverCommunicationSocket.disconnect_from_host()
	Globals.serverBroadcastSocket.disconnect_from_host()
	Globals.serverBroadcastSocket = null
	Globals.serverCommunicationSocket = null
	Globals.isServerConnected = false
	Globals.isNetworkConfigured = false

static func closeUDP():
	if !Globals.isUDPConfigured:
		return
	Globals.udpCommunicationSocket.close()
	print("socket fechado de comunicação udp: ", EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao)
	UPNPJava.deletePortMapping(EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao, EnvironmentVariables.portaDoSocketDeRecebimentoDePacotesUDPPadrao, "udp")
	Globals.udpCommunicationSocket = null
	Globals.isUDPConfigured = false
	Globals.isNetworkConfigured = false

static func closeAllSockets():
	closeConnectionToServer()
	closeUDP()
	Globals.isNetworkConfigured = false
