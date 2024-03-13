import socket

HOST = 'localhost'
PORT = 7659
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST,PORT))
    data = s.recv(1024).decode('utf-8')
    print('Conexao realizada com sucesso! Data e hora da conexão: ',data, '\n\n')
    while True:
        print('Escreva a mensagem que você quer enviar para o servidor: ',end = '')
        entrada = str(input())
        if(entrada == 'finalizar'):
            s.send(b'finalizar')
            break
        else:
            s.send(bytes(entrada,'utf-8'))
            data = s.recv(61).decode('utf-8')
            print(data)
            data = s.recv(1024).decode('utf-8')
            print("Servidor: ",data, '\n\n\n')
    
    data = s.recv(1024).decode('utf-8')
    print("Servidor: ",data, '\n\n\n')
