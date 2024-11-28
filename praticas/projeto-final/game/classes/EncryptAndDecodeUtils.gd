class_name EncryptAndDecodeUtils extends Node


static func decryptMessage(message:String) -> PackedByteArray:
	var decriptador:AESContext = AESContext.new()
	decriptador.start(AESContext.MODE_CBC_DECRYPT, EnvironmentVariables.key.to_ascii_buffer(), EnvironmentVariables.iv.to_ascii_buffer())
	var decodedBuffer:PackedByteArray = decode_base64(message)
	if decodedBuffer.size() % 16 != 0:
		var decryptedBuffer = decriptador.update(decodedBuffer)
		decriptador.finish()
		unpad(decryptedBuffer)
		return decryptedBuffer
	else:
		var decryptedBuffer = decriptador.update(decodedBuffer)
		decriptador.finish()
		unpad(decryptedBuffer)
		return decryptedBuffer


static func encryptMessage(message: String) -> PackedByteArray:
	var encriptador: AESContext = AESContext.new()
	encriptador.start(AESContext.MODE_CBC_ENCRYPT, EnvironmentVariables.key.to_ascii_buffer(), EnvironmentVariables.iv.to_ascii_buffer())

	# Codifica a mensagem para um buffer de bytes
	var messageBuffer: PackedByteArray = message.to_utf8_buffer()

	# Adiciona padding para que o tamanho do buffer seja múltiplo do base
	var paddedBuffer: PackedByteArray = pad(messageBuffer)

	# Criptografa o buffer
	var encryptedBuffer: PackedByteArray = encriptador.update(paddedBuffer)
	encriptador.finish()

	# Codifica o buffer criptografado em base64 para facilitar a transmissão
	var encodedBuffer: String = Marshalls.raw_to_base64(encryptedBuffer)

	return encodedBuffer.to_utf8_buffer()


static func pad(bytes: PackedByteArray) -> PackedByteArray:
	var n = 16 - bytes.size() % 16
	var offset:int = (((bytes.size() / 16) + 1) *16) - n	
	for i in range(offset,offset+n):
		bytes.append(n)			
	return bytes


static func unpad(padded_data: PackedByteArray) -> void:
	var inLen = padded_data.size()
	if inLen == 0:
		push_error(error_string(1))
		return
	var padChar = padded_data[inLen - 1]
	if padChar > 16:
		push_error(error_string(1))
		return
	for i in range(inLen - padChar, inLen):
		if padded_data[i] != padChar:
			push_error(error_string(1))
			return
	padded_data.resize(inLen - padChar)


static func decode_base64(encoded_data: String) -> PackedByteArray:
	var decoded_bytes: PackedByteArray = PackedByteArray()
	var base64_chars: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
	var padding_char: String = "="

	var buffer: int = 0
	var buffer_size: int = 0

	for c in encoded_data:
		if c == padding_char:
			break
		var char_index: int = base64_chars.find(c)
		if char_index >= 0:
			buffer = (buffer << 6) | char_index
			buffer_size += 6
			while buffer_size >= 8:
				var byte_value: int = buffer >> (buffer_size - 8)
				decoded_bytes.append(byte_value)
				buffer_size -= 8
				buffer &= (1 << buffer_size) - 1
	return decoded_bytes