extends Control

# Called when the node enters the scene tree for the first time.
func _ready():
	Globals.recieverSocket = PacketPeerUDP.new()
	Globals.recieverSocket.bind(0,"*",2048)
	
	print(Globals.recieverSocket.is_bound())
	
	Globals.fazerUpnp(Globals.recieverSocket.get_port())
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	# 9223372012460702513
	# 9223372012460702513
	# 9223372012460702513
	pass
