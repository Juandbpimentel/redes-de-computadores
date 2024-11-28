class_name Player2 extends CharacterBody2D


var speed = 200
var health = 1
var atacando = false
var andando = false
var defendendo = false
@onready var spriteSheet = $Sprite2D



# Called when the node enters the scene tree for the first time.

@onready var animation := $AnimationPlayer as AnimationPlayer

func _ready() -> void:
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(_delta: float) -> void:
	pass


func _physics_process(_delta):
	if Globals.isPlayer1 == true:
			return
	movement_and_animation()
	setar_animacao()
	move_and_slide()
	pass

func movement_and_animation():

		if !((Input.is_action_pressed('ui_left') && !Input.is_action_pressed('ui_right')) || (Input.is_action_pressed('ui_right') && !Input.is_action_pressed('ui_left'))):
			velocity.x = 0
			andando = false
			return
	
		if Input.is_action_pressed('ui_right'):
			andando = true
			velocity.x = speed
			defendendo = true
		
		if Input.is_action_pressed('ui_left'):
			andando = true
			velocity.x = -speed
			defendendo = false
		else:
			defendendo = false

		if !Input.is_action_pressed('ui_right') && !Input.is_action_pressed('ui_left'):
			andando = false
			defendendo = false
		
		if Input.is_action_just_pressed('ui_select'):
			atacando = true
			if defendendo == true:
				defendendo = false
		else:
			atacando = false
		print("defendendo: ", defendendo)
		
			

func setar_animacao():
	var state = "parado"
	
	if andando:
		state = "andando"
	if defendendo:
		state = "defendendo"
	if atacando:
		state = "atacando"
	
	if animation.name != state:
		animation.play(state)

func _on_area_2d_body_entered(body:Node2D) -> void:
	if body is Player:
		if !body.defendendo:
			health -= 1
			if health == 0:
				finish_game()

func finish_game():
	get_tree().change_scene_to_file("res://scenes/MenuPrincipal.tscn")
