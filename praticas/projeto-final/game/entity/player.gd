class_name Player extends CharacterBody2D

var speed = 200
var walking = false
var atacking = false
var health = 1
var atacando = false
var andando = false
var defendendo = false
var direcao = Vector2(0, 0)
@onready var spriteSheet = $Sprite2D



# Called when the node enters the scene tree for the first time.

@onready var animation := $AnimationPlayer as AnimationPlayer

func _ready() -> void:
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(_delta: float) -> void:
	pass


func _physics_process(_delta):
	if Globals.isPlayer1 == false:
			return
	movement_and_animation()
	move_and_slide()
	setar_animacao()
	pass

func movement_and_animation():
		if !((Input.is_action_pressed('ui_left') && !Input.is_action_pressed('ui_right')) 
		|| (Input.is_action_pressed('ui_right') && !Input.is_action_pressed('ui_left'))):
			velocity.x = 0
			andando = false
			return
	
		if Input.is_action_pressed('ui_right'):
			walking = true
			velocity.x = speed
			defendendo = false

		if Input.is_action_pressed('ui_left'):
			walking = true
			velocity.x = -speed
			defendendo = true
			

		if Input.is_action_pressed('ui_select'):
			atacando = true
			defendendo = false
		else:
			atacando = false
			

func setar_animacao():
	var state = "parado"
	if atacando:
		state = "atacando"
	elif defendendo:
		state = "defendendo"
	elif andando:
		state = "andando"
	
	if animation.name != state:
		animation.play(state)

func _on_area_2d_body_entered(body:Node2D) -> void:
	if body is Player2:
		if !body.defendendo:
			health -= 1
			if health == 0:
				finish_game()

func finish_game():
	get_tree().change_scene_to_file("res://scenes/MenuPrincipal.tscn")
