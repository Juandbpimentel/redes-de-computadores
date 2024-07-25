import socket
import struct


def udp_server(addr):
    soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    soc.bind(addr)
    print('esperando conexão...')
    _, client_a = soc.recvfrom(0)
    _, client_b = soc.recvfrom(0)
    print('conectado:', client_a, client_b)
    soc.sendto(str(client_b).encode(), client_a)
    soc.sendto(str(client_a).encode(), client_b)


print('rodou')
address = ('0.0.0.0', 10001)
udp_server(address)
