import socket

HOST = 'localhost'  # Endereco IP do Servidor
PORT = 10001            # Porta que o Servidor esta
udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
dest = (HOST, PORT)

print('Para sair use CTRL+X\n')
while True:
    msg = str(input("digite aqui a mensagem para enviar: "))
    if msg == 'sair':
        msg = 'sair'
        udp.sendto (msg.encode(), dest)
        break
    udp.sendto (msg.encode(), dest)
udp.close()