class_name OperationRequestBody extends Node

var operation: String
var data:Variant
var isEncrypted: bool

func _init(initParams: Variant):
	if initParams == null:
		self.operation = ""
		self.data = {}
		self.isEncrypted = false
	elif initParams is Dictionary:
		parse_dictionary_to_operation(initParams)
	elif initParams is String:
		parse_json_string_to_operation(initParams)
	elif initParams is OperationRequestBody:
		self.operation = initParams.operation
		self.data = initParams.data
		self.isEncrypted = initParams.isEncrypted
	else:
		self.operation = ""
		self.data = {}
		self.isEncrypted = false

## Essa função server para transformar o objeto OperationRequestBody em uma string
## para que possa ser enviado pela rede
func stringify() -> String:
	return JSON.stringify(dictionarify(true))

## Essa função server para transformar o objeto OperationRequestBody em um objeto Dictionary
## para que possa facilitar a manipulação dos dados
func dictionarify(dataAsString:bool) -> Dictionary:
	return {
		"operation": self.operation,
		"data": self.get_data_as_string() if dataAsString else self.get_data_as_dictionary(),
		"isEncrypted": self.isEncrypted
	}

## Essa função server para transformar uma string em formato de json em um objeto OperationRequestBody
func parse_json_string_to_operation(json: String) -> void:
	var jsonParser = JSON.new()
	var err = jsonParser.parse(json)
	if err != OK:
		push_error("erro ao tentar parsear o json")
		return
	self.operation = jsonParser.data["operation"]
	self.data = jsonParser.data["data"]
	self.isEncrypted = jsonParser.data["isEncrypted"]

## Essa função server para transformar uma string em formato de json em um objeto OperationRequestBody
func parse_dictionary_to_operation(json: Dictionary) -> void:
	self.operation = json["operation"]
	self.data = json["data"]
	self.isEncrypted = json["isEncrypted"]

## Essa função server para transformar a propriedade data em um objeto String
func get_data_as_string():
	if self.data == null:
		return null
	if self.data is Dictionary:
		return JSON.stringify(self.data)
	else:
		return self.data

## Essa função server para transformar a propriedade data em um objeto Dictionary
func get_data_as_dictionary() -> Dictionary:
	if self.data == null:
		return {}
	if self.data is Dictionary:
		return self.data
	else:
		var jsonParser = JSON.new()
		var err = jsonParser.parse(self.data)
		if err != OK:
			push_error("erro ao tentar parsear o json")
			return {}
		return jsonParser.data

## Essa função server para salvar dados na propriedade data
func set_data(requestData: Variant) -> void:
	self.data = requestData

## Essa função server para criptografar os dados da propriedade data
func encrypt_data() -> void:
	if self.isEncrypted || self.data == null:
		return
	self.data = EncryptAndDecodeUtils.encryptMessage(get_data_as_string()).get_string_from_utf8()
	isEncrypted = true

## Essa função server para descriptografar os dados da propriedade data
func decrypt_data() -> void:
	if !self.isEncrypted || self.data == null:
		return
	var jsonParser = JSON.new()
	var err = jsonParser.parse(EncryptAndDecodeUtils.decryptMessage(get_data_as_string()).get_string_from_utf8())
	if err != OK:
		push_error("erro ao tentar parsear o json")
		return
	self.data = jsonParser.data
	isEncrypted = false

func get_operation() -> String:
	return self.operation

func set_operation(requestOperation: String) -> void:
	self.operation = requestOperation

func get_is_encrypted() -> bool:
	return self.isEncrypted

func set_is_encrypted(requestIsEncrypted: bool) -> void:
	self.isEncrypted = requestIsEncrypted
