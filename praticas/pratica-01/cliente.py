import socket

print("Aplicação de cliente do servidor que mostra a hora")
print("Diga aqui o endereço do host: ", end = "")
HOST = str(input())
print("Agora fale a porta da aplicação no host: ",end = "")
PORT = int(input())

print('Inicializando a conexão...')
try:
	with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
	    s.connect((HOST,PORT))
	    data = s.recv(1024).decode('utf-8')
	    print('Conexao realizada com sucesso! Data e hora: ',repr(data))
	
except Exception as error:
	print('Erro de conexão')
	
