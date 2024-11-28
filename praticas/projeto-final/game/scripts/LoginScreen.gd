extends Node2D


var nickname = ""
var password = ""
var logged = false
var waitingServerResponse = false
var localThreads:Array[Thread] = []
@onready var info_label = $MenuControl/Info_Label
var err = Globals.LoginScreenErrors.NONE

func _ready():
	info_label.visible = false
	_verificar_configuracao_rede()
	
func _process(_delta):
	if err != Globals.LoginScreenErrors.NONE:
		_handle_errors()
		err = Globals.LoginScreenErrors.NONE
	pass


func _verificar_configuracao_rede():
	if Globals.isNetworkConfigured == false:
		var thread = Thread.new()
		thread.start(NetworkUtils.alocAllSockets)
		Globals.configGlobalThreads.append(thread)
		OS.delay_msec(100)
	for thread in Globals.configGlobalThreads:
		if thread.is_alive():
			thread.wait_to_finish()


func _fazer_loginOuRegistro(nicknamePassado, senhaPassada, isLogin:bool) -> void:
	_verificar_configuracao_rede()
	var msg = {"operation":"connect", "data":null, "isEncrypted":false}


	err = NetworkUtils.sendMessageWithUDPCommunicationSocket(msg, EnvironmentVariables.enderecoDoServidorPadrao, EnvironmentVariables.portaDoServidorPadrao, true)
	if err != OK:
		match err:
			Globals.LoginScreenErrors.timeout:
				push_error("O Servidor está inacessível, tente novamente mais tarde")
				NetworkUtils.closeAllSockets()
				err = Globals.LoginScreenErrors.falha_ao_enviar_pacote_udp
				return
			_:
				push_error("Erro ao tentar enviar pacote de conexão ao servidor")
				NetworkUtils.closeAllSockets()
				err = Globals.LoginScreenErrors.falha_ao_enviar_pacote_udp
				return

	var response:Variant = NetworkUtils.recieveMessageWithUDPCommunicationSocket()
	if !response is OperationRequestBody:
		err = response
		match err:
			Globals.LoginScreenErrors.timeout:
				push_error("O Servidor está inacessível, tente novamente mais tarde")
				NetworkUtils.closeAllSockets()
				return
			Globals.LoginScreenErrors.erro_ao_esperar_pacote:
				push_error("Erro ao receber portas para conexão com o servidor, tente novamente mais tarde")
				NetworkUtils.closeAllSockets()
				return
			_:
				push_error("Erro ao receber portas para conexão com o servidor")
				print("erro ao receber pacote: ", err)
				NetworkUtils.closeAllSockets()
				return


	response.decrypt_data()
	if response.operation != "establish_connection":
		push_error("Erro ao receber portas para conexão com o servidor")
		NetworkUtils.closeAllSockets()
		err = Globals.LoginScreenErrors.erro_ao_estabelecer_conexao
		return
	

	NetworkUtils.defineServerPortsAndAdress(EnvironmentVariables.enderecoDoServidorPadrao, response.data["serverPort"])
	err = NetworkUtils.connectToServer()
	if err != OK:
		push_error("Erro ao tentar estabelecer conexão com o servidor")
		NetworkUtils.closeAllSockets()
		err = Globals.LoginScreenErrors.erro_ao_estabelecer_conexao
		return
	print("conectado ao servidor")
	print("portas do servidor: ", response.data["serverPort"])


	if isLogin:
		msg = {"operation":"login", "data":{"nickname": nicknamePassado,"password": senhaPassada}, "isEncrypted":false}
	else:
		msg = {"operation":"register", "data":{"nickname": nicknamePassado,"password": senhaPassada}, "isEncrypted":false}
	err = NetworkUtils.sendMessageWithTCPCommunicationSocket(msg)
	if err != OK:
		push_error("Erro ao tentar enviar operação de login ou registro ao servidor")
		NetworkUtils.closeAllSockets()
		err = Globals.LoginScreenErrors.falha_ao_enviar_pacote_tcp
		return


	response = NetworkUtils.recieveMessageWithTCPCommunicationSocket(2048)
	if response == null || response.operation.contains("error") || response.operation.contains("fail"):
		NetworkUtils.closeAllSockets()
		if response != null:
			var data:Dictionary = response.get_data_as_dictionary()
			match data.get("msg"):
				"Já existe um usuário com esse nickname":
					err = Globals.LoginScreenErrors.usuario_ja_existe
					push_error("Erro ao tentar se registrar, usuário já existe")
					return

				"Não existe um usuário com esse nickname":
					err = Globals.LoginScreenErrors.usuario_nao_encontrado
					push_error("Erro durante o login, usuário com esse nickname não existe")
					return
				
				"Sua senha não está certa":
					err = Globals.LoginScreenErrors.senha_incorreta
					push_error("Erro durante o login, a senha está incorreta")				
					return
				
				"Erro inesperado ao fazer login ou registro":
					err = Globals.LoginScreenErrors.erro_inexperado_ao_fazer_login_ou_registro
					push_error("Erro inesperado durante o login ou registro, tente novamente mais tarde")
					return
				
				"Usuário já está logado":
					err = Globals.LoginScreenErrors.usuario_ja_esta_logado
					push_error("Erro ao tentar se logar, usuário já está logado")
					return
				_:
					err = Globals.LoginScreenErrors.erro_inexperado_ao_fazer_login_ou_registro
					push_error("Erro inesperado durante o login ou registro, tente novamente mais tarde")
					return
		else:
			err = Globals.LoginScreenErrors.erro_inexperado_ao_fazer_login_ou_registro
			push_error("Erro inesperado durante o login ou registro, tente novamente mais tarde")
			return


	response.decrypt_data()
	print("resposta do servidor: { operacao: ", response.operation, ", data: {", response.data,"}, isEncrypted: ", response.isEncrypted, "}")
	msg = {"operation":"ok", "data":"", "isEncrypted":false}
	
	
	NetworkUtils.sendMessageWithTCPCommunicationSocket(msg)
	
	
	print("Login feito com sucesso! Nickname: ", nickname," | Senha: ", password)
	if isLogin:
		err = Globals.LoginScreenErrors.login_sucesso
	else:
		err = Globals.LoginScreenErrors.registro_sucesso
	Globals.userToken = response.data["userToken"]
	Globals.status = Globals.StatusEnum.online
	print("userToken: ", Globals.userToken)
	logged = true
	


func _on_login_button_button_down() -> void:
	if waitingServerResponse:
		print("aguardando resposta do servidor")
		return
	info_label.text = "Se Conectando com o servidor, espere um pouco..."
	info_label.visible = true
	info_label.add_theme_color_override("font_color",Color(1,1,1))
	nickname = $MenuControl/Nickname.text
	password = $MenuControl/Password.text
	waitingServerResponse = true
	var registerThread:Thread = Thread.new()
	registerThread.start(_fazer_loginOuRegistro.bind(nickname,password,true))
	localThreads.append(registerThread)



func _on_register_button_button_down() -> void:
	if waitingServerResponse:
		print("aguardando resposta do servidor")
		return
	info_label.text = "Se Conectando com o servidor, espere um pouco..."
	info_label.visible = true
	info_label.add_theme_color_override("font_color",Color(1,1,1))
	nickname = $MenuControl/Nickname.text
	password = $MenuControl/Password.text
	waitingServerResponse = true
	var registerThread:Thread = Thread.new()
	registerThread.start(_fazer_loginOuRegistro.bind(nickname,password,false))
	localThreads.append(registerThread)


func _handle_errors():
	if err == Globals.LoginScreenErrors.login_sucesso:
		info_label.text = "Login feito com sucesso!"
		info_label.visible = true
		info_label.add_theme_color_override("font_color",Color(0,1,0))
		logged = true
		waitingServerResponse = false
		get_tree().change_scene_to_file("res://scenes/MenuPrincipal.tscn")
		err = Globals.LoginScreenErrors.NONE
		return
	elif err == Globals.LoginScreenErrors.registro_sucesso:
		info_label.text = "Registro feito com sucesso!"
		info_label.visible = true
		info_label.add_theme_color_override("font_color",Color(0,1,0))
		logged = true
		waitingServerResponse = false
		get_tree().change_scene_to_file("res://scenes/MenuPrincipal.tscn")
		err = Globals.LoginScreenErrors.NONE
		return
	match err:
		Globals.LoginScreenErrors.timeout, Globals.LoginScreenErrors.erro_ao_esperar_pacote:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "O Servidor está inacessível, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.falha_ao_enviar_pacote_tcp:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar enviar pacote de login ao servidor"
			info_label.visible = true
		Globals.LoginScreenErrors.falha_ao_enviar_pacote_udp:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar enviar pacote de conexão ao servidor"
			info_label.visible = true
		Globals.LoginScreenErrors.resposta_nula:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar receber mensagem do servidor, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.erro_ao_estabelecer_conexao:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar se conectar, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.resposta_inesperada_do_servidor:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro inesperado durante o login ou registro, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.erro_inexperado_ao_fazer_login_ou_registro:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro inesperado durante o login ou registro, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.resposta_inesperada_do_servidor:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro inesperado durante o login ou registro, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.usuario_ja_existe:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar se registrar, usuário já existe"
			info_label.visible = true
		Globals.LoginScreenErrors.usuario_nao_encontrado:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro durante o login, usuário com esse nickname não existe"
			info_label.visible = true
		Globals.LoginScreenErrors.senha_incorreta:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro durante o login, a senha está incorreta"
			info_label.visible = true
		Globals.LoginScreenErrors.resposta_nula:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar receber mensagem do servidor, tente novamente mais tarde"
			info_label.visible = true
		Globals.LoginScreenErrors.usuario_ja_esta_logado:
			info_label.add_theme_color_override("font_color",Color(1,0,0))
			info_label.text = "Erro ao tentar se logar, usuário já está logado"
			info_label.visible = true

	err = Globals.LoginScreenErrors.NONE
	logged = false
	waitingServerResponse = false
	

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		Globals.abortThreads = true
		for thread:Thread in localThreads:
			thread.wait_to_finish()
			print("thread terminada")
		for thread:Thread in Globals.configGlobalThreads:
			thread.wait_to_finish()
			print("thread terminada")
		NetworkUtils.closeAllSockets()
		_on_tree_exiting()
		get_tree().quit()

func _on_tree_exiting() -> void:
	print("saindo da cena")


func _on_return_button_button_down() -> void:
	print("saindo do game")
	if waitingServerResponse:
		print("aguardando resposta do servidor")
		info_label.add_theme_color_override("font_color",Color(1,1,1))
		info_label.text = "Se Conectando com o servidor, espere um pouco..."
		info_label.visible = true
		return
	notification(NOTIFICATION_WM_CLOSE_REQUEST)
	pass # Replace with function body.
