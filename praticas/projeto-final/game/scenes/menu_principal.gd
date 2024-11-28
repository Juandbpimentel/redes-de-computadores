extends Node2D

var goToLobby = false
var killBroadcastThread = false

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	$".".get_node("MenuControl/Status_Label").hide()
	var thready = Thread.new()
	thready.start(_escutar_mensagens_do_server)
	Globals.genericThreads.append(thready)
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	if goToLobby:
		get_tree().change_scene_to_file("res://scenes/Duel.tscn")
		goToLobby = false
	pass


func _logout() -> void:
	var err =  NetworkUtils.sendMessageWithTCPCommunicationSocket({"operation": "logout", "data":{"msg": null}, "isEncrypted": false})
	if err == Globals.LoginScreenErrors.falha_ao_enviar_pacote_tcp:
		$".".get_node("Status_Label").text = "Erro ao tentar fazer logout"
		return
	print("Deslogando")
	pass # Replace with function body.

func _on_logout_button_button_down() -> void:
	var thready = Thread.new()
	thready.start(_logout)
	thready.wait_to_finish()
	killBroadcastThread = true
	$".".get_node("MenuControl/Status_Label").text = "Deslogando..."
	$".".get_node("MenuControl/Status_Label").show()
	NetworkUtils.closeAllSockets()
	get_tree().change_scene_to_file("res://scenes/LoginScreen.tscn")
	pass # Replace with function body.

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		Globals.abortThreads = true
		for thread:Thread in Globals.configGlobalThreads:
			thread.wait_to_finish()
			print("thread terminada")
		NetworkUtils.closeAllSockets()
		get_tree().quit()


func _on_ver_ranking_button_button_down() -> void:
	pass # Replace with function body.


func _on_batalha_aleatoria_button_button_down() -> void:
	pass # Replace with function body.

func _on_desafiar_button_button_down() -> void:
	$PopupDesafio.show()
	pass

func _escutar_mensagens_do_server():
	while !Globals.abortThreads && !killBroadcastThread:
		print("Escutando mensagens do server")
		if Globals.abortThreads || killBroadcastThread:
			break
		var response = NetworkUtils.recieveMessageWithTCPBroadcastSocket(1024)
		var responseData = OperationRequestBody.new(response)
		if responseData == null:
			continue
		responseData.decrypt_data()
		responseData.set_data(responseData.get_data_as_dictionary())
		if responseData.operation.contains("error") || responseData.operation.contains("fail"):
			$".".get_node("Status_Label").text = response.data["msg"]
			$".".get_node("Status_Label").show()
			continue
		if responseData.operation == "challenge_invite":
			if responseData.data != null:
				Globals.portaSocketParDueloUDP = responseData.data["challengerPort"]
				Globals.enderecoParDuelo = responseData.data["challengerIp"]
				Globals.nicknameParDuelo = responseData.data["challengerNick"]
				Globals.isPlayer1 = responseData.data["isPlayerOne"]
				print("Nick do desafiante: ", Globals.nicknameParDuelo, " porta: ", Globals.portaSocketParDueloUDP, " ip: ", Globals.enderecoParDuelo)
				print("Sou player 1? ", Globals.isPlayer1)
				print("Desafio aceito por broadcast")
				goToLobby = true
				continue
			else:
				$".".get_node("Status_Label").text = "Erro ao tentar desafiar"
				$".".get_node("Status_Label").show()
				continue
		if responseData.operation == "closed_server":
			$".".get_node("Status_Label").text = "Servidor fechado"
			Globals.abortThreads = true
			NetworkUtils.closeAllSockets()
			get_tree().quit()
