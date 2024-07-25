import socket
import struct


def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])


def bytes_to_addr(addr):
    return socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0]


def udp_server(addr):
    soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    soc.bind(addr)
    print('esperando conexão...')
    _, client_a = soc.recvfrom(0)
    _, client_b = soc.recvfrom(0)
    print('conectado:', client_a, client_b)
    #code to transform client tuple in string
    soc.sendto(str(client_b).encode(), client_a)
    soc.sendto(str(client_a).encode(), client_b)


print('rodou')
address = ('0.0.0.0', 10001)
udp_server(address)
