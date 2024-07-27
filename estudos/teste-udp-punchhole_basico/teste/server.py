import socket
import struct
import time


def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])


def bytes_to_addr(addr):
    return socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0]


def udp_server(soc, addr):
    soc.bind(addr)
    print('esperando conexão...')
    
    _, client_a_reciever_adress = soc.recvfrom(0)
    _, client_b_reciever_adress = soc.recvfrom(0)
    
    print('Clientes conectados:', client_a_reciever_adress, client_b_reciever_adress)
    soc.sendto(str(client_b_reciever_adress).encode(), client_a_reciever_adress)
    soc.sendto(str(client_a_reciever_adress).encode(), client_b_reciever_adress)


soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
print('rodou')
address = ('0.0.0.0', 10001)
while True:
    print('rodando')
    udp_server(soc,address)
