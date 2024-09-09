extends Control

var nickname = ""
var password = ""
var logged = false
var waitingServerResponse = false
var localThreads:Array[Thread] = []


func _ready():
	_verificar_configuracao_rede()

	for thread in Globals.configGlobalThreads:
		thread.wait_to_finish()
	print("Configurações do cliente realizadas")
	
func _process(_delta):
	pass


func _verificar_configuracao_rede():
	if Globals.isNetworkConfigured == false:
		var thread = Thread.new()
		thread.start(NetworkUtils.alocAllSockets)
		Globals.configGlobalThreads.append(thread)


func _fazer_loginOuRegistro(nicknamePassado, senhaPassada, isLogin:bool) -> void:
	_verificar_configuracao_rede()
	for thread in Globals.configGlobalThreads:
		thread.wait_to_finish()
	print("Configurações do cliente realizadas")
	print("rede configurada"  if Globals.isNetworkConfigured else "rede não configurada")
	var msg = {"operation":"connect", "data":null, "isEncrypted":false}

	NetworkUtils.sendMessageWithUDPCommunicationSocket(msg, EnvironmentVariables.enderecoDoServidorPadrao, EnvironmentVariables.portaDoServidorPadrao, true)
	
	var response:OperationRequestBody = NetworkUtils.recieveMessageWithUDPCommunicationSocket()
	if response == null:
		push_error("resposta nula")
		call_deferred("login_error", "resposta nula")
		print("resposta nula")
		NetworkUtils.closeAllSockets()
		waitingServerResponse = false
		return

	response.decrypt_data()

	if response.operation != "establish_connection":
		print("houve um erro ao tentar fazer login")
		logged = false
		NetworkUtils.closeAllSockets()
		waitingServerResponse = false
		return
	
	NetworkUtils.defineServerPortsAndAdress(EnvironmentVariables.enderecoDoServidorPadrao, response.data["comunicationPort"], response.data["broadcastPort"])
	var err = NetworkUtils.connectToServer()
	if err != OK:
		call_deferred("erro ao tentar conectar ao servidor")
		print("erro ao tentar conectar ao servidor")
		NetworkUtils.closeAllSockets()
		waitingServerResponse = false
		return
	print("conectado ao servidor")
	print("portas do servidor: ", response.data["comunicationPort"], " | ", response.data["broadcastPort"])

	if isLogin:
		msg = {"operation":"login", "data":{"nickname": nicknamePassado,"password": senhaPassada}, "isEncrypted":false}
	else:
		msg = {"operation":"register", "data":{"nickname": nicknamePassado,"password": senhaPassada}, "isEncrypted":false}
	OS.delay_msec(1000)
	NetworkUtils.sendMessageWithTCPCommunicationSocket(msg)
	response = NetworkUtils.recieveMessageWithTCPCommunicationSocket(2048)
	if response == null || response.operation.contains("error") || response.operation.contains("fail"):
		if response != null:
			var data:Dictionary = response.get_data_as_dictionary()
			match data.get("msg"):
				"Já existe um usuário com esse nickname":
					call_deferred("usuário já existe")
					print("usuário já existe")
					NetworkUtils.closeAllSockets()
					waitingServerResponse = false
					return

				"Não existe um usuário com esse nickname":
					call_deferred("usuario não encontrado")
					print("usuario não encontrado")
					NetworkUtils.closeAllSockets()
					waitingServerResponse = false
					return
				
				"Sua senha não está certa":
					call_deferred("senha incorreta")
					print("senha incorreta")
					NetworkUtils.closeAllSockets()
					waitingServerResponse = false
					return
				
				"Erro inexperado ao fazer ou registro":
					call_deferred("login_error", "Erro inexperado ao fazer ou registro")
					print("Erro inexperado ao fazer ou registro")
					NetworkUtils.closeAllSockets()
					waitingServerResponse = false
					return

				_:
					call_deferred("login_error", "resposta inesperada do servidor")
					print("resposta inesperada do servidor")
					NetworkUtils.closeAllSockets()
					waitingServerResponse = false
					return
		else:
			print("resposta nula")
			push_error("resposta nula")
			print("resposta nula")
			NetworkUtils.closeAllSockets()
			waitingServerResponse = false
			return
	response.decrypt_data()
	print("resposta do servidor: { operacao: ", response.operation, ", data: {", response.data,"}, isEncrypted: ", response.isEncrypted, "}")
	msg = {"operation":"ok", "data":"", "isEncrypted":false}
	NetworkUtils.sendMessageWithTCPCommunicationSocket(msg)
	logged = true
	waitingServerResponse = false
	print("Login feito com sucesso! Nickname: ", nickname," | Senha: ", password)
	$DeuCertoLoginLabel.visible = true
	


func _on_login_button_button_down() -> void:
	if waitingServerResponse:
		print("aguardando resposta do servidor")
		return
	nickname = $Nickname.text
	password = $Password.text
	waitingServerResponse = true
	var thread = Thread.new()
	thread.start(_fazer_loginOuRegistro.bind(nickname,password,true))
	localThreads.append(thread)
		

func on_tree_exiting():
	for thread in localThreads:
		thread.free()
		print("thread assassinada")
	Globals.recieverSocket.close()
	Globals.recieverSocket.free()
	print("saindo da cena")
	get_tree().quit()


func _on_register_button_button_down() -> void:
	if waitingServerResponse:
		print("aguardando resposta do servidor")
		return
	nickname = $Nickname.text
	password = $Password.text
	waitingServerResponse = true
	var thread = Thread.new()
	thread.start(_fazer_loginOuRegistro.bind(nickname,password,false))
	localThreads.append(thread)
