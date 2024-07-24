import socket
import struct
from threading import Thread
import sys

def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])

def bytes_to_addr(addr):
    return (socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0])

def udp_client(server):
    soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    soc.sendto(b'', server)
    data, _ = soc.recvfrom(6)
    peer = bytes_to_addr(data)
    print('peer:', *peer)

    soc.sendto(b'Hello, peer!', peer)
    data, addr = soc.recvfrom(1024)
    print('{}:{} says {}'.format(*addr, data))

host = sys.argv[1]
port = int(sys.argv[2])
print("tentando conectar to {}:{}".format(host, port))
server_addr = (host, port) # the server's  public address
udp_client(server_addr)
print("conectado")