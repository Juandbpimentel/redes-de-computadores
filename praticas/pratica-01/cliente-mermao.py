import socket

print("teste")
HOST = 'localhost'
PORT = 7658
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST,PORT))
    data = s.recv(1024)
    print('Conexao realizada com sucesso! Data e hora: ',repr(data))