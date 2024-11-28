extends Window

@onready var nickname = $Nickname_Edit
@onready var status = $Status_label

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(_delta: float) -> void:
	pass


func _on_close_requested() -> void:
	$".".hide()
	pass # Replace with function body.


func _on_desafiar_button_button_down() -> void:
	if nickname.text == "":
		status.add_theme_color_override("font_color", Color(1, 0, 0))
		status.text = "O Nickname não pode ser vazio"
		status.show()
	if nickname.text == Globals.userToken["nickname"]:
		status.add_theme_color_override("font_color", Color(1, 0, 0))
		status.text = "Você não pode desafiar a si mesmo"
		status.show()
	status.add_theme_color_override("font_color", Color(1, 1, 1))
	status.text = "Desafiando..."
	status.show()
	var message = {"operation": "challenge", "data": {"challenged": nickname.text, "challenger":Globals.userToken["nickname"]}, "isEncrypted": false}
	NetworkUtils.sendMessageWithTCPCommunicationSocket(message)
	var response = NetworkUtils.recieveMessageWithTCPCommunicationSocket(1024)
	var responseData:OperationRequestBody = OperationRequestBody.new(response)
	responseData.decrypt_data()
	responseData.set_data(responseData.get_data_as_dictionary())
	print("resposta do desafio: ", responseData.stringify())
	if responseData == null:
		status.add_theme_color_override("font_color", Color(1, 0, 0))
		status.text = "Erro ao tentar desafiar"
		status.show()
		print("resposta nula")
		return
	if responseData.operation.contains("error") || responseData.operation.contains("fail"):
		status.add_theme_color_override("font_color", Color(1, 0, 0))
		status.text = responseData.get_data_as_dictionary()["msg"]
		status.show()
		return
	if responseData.operation == "challenge_invite":
		$".".hide()
		if response.data != null:
			Globals.portaSocketParDueloUDP = responseData.data["challengerPort"]
			Globals.enderecoParDuelo = responseData.data["challengerIp"]
			Globals.nicknameParDuelo = responseData.data["challengerNick"]
			Globals.isPlayer1 = responseData.data["isPlayerOne"]
			print("Nick do desafiante: ", Globals.nicknameParDuelo, " porta: ", Globals.portaSocketParDueloUDP, " ip: ", Globals.enderecoParDuelo)
			print("Sou player 1? ", Globals.isPlayer1)
			print("Desafio aceito")
			get_tree().change_scene_to_file("res://scenes/Duel.tscn")
		else:
			status.add_theme_color_override("font_color", Color(1, 0, 0))
			status.text = "Erro ao tentar desafiar"
			status.show()
	return
