import socket


def udp_server(soc:socket.socket):
    print('esperando conexão...')
    _, client_a_reciever_adress = soc.recvfrom(0)
    _, client_b_reciever_adress = soc.recvfrom(0)
    
    print('Clientes conectados:', client_a_reciever_adress, client_b_reciever_adress)
    soc.sendto(str(client_b_reciever_adress).encode(), client_a_reciever_adress)
    soc.sendto(str(client_a_reciever_adress).encode(), client_b_reciever_adress)


soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
address = ('0.0.0.0', 10001)
soc.bind(address)
print('rodou')
while True:
    print('rodando')
    udp_server(soc)
