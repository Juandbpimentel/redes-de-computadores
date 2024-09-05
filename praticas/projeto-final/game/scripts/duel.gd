extends Node2D


# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(_delta: float) -> void:
	var msg = "???"
	var bytes = msg.to_utf8_buffer()
	
	if bytes.size() % 4 != 0:
		_pad(bytes,4)
	

func _pad(bytes: PackedByteArray, base: int) -> PackedByteArray:
	var n = base - bytes.size() % base
	var offset:int = (((bytes.size() / base) + 1) *base) - n	
	
	for i in range(offset,offset+n):
		bytes[i] = n
					
	return bytes
