import socket
import struct
import sys
import time

def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])

def bytes_to_addr(addr):
    return (socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0])

def udp_client(server, message):
    try:
        soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        print("Enviando pacote vazio para o servidor...")
        soc.sendto(b'', server)
        
        print("Aguardando resposta do servidor...")
        data, _ = soc.recvfrom(6)
        peer = bytes_to_addr(data)
        print('peer:', *peer)
        data, _ = soc.recvfrom(1)
        if data.decode() == '2':
            print("Aguardando resposta do peer...")
            data, addr = soc.recvfrom(2048)
            print('{}:{} says {}'.format(*addr, data.decode()))
            time.sleep(2)
            print("Enviando mensagem para o peer...")
            soc.sendto(message.encode(), peer)
            return
        
        if data.decode() == '1':
            print("Enviando mensagem para o peer...")
            soc.sendto(message.encode(), peer)
            
            print("Aguardando resposta do peer...")
            data, addr = soc.recvfrom(2048)
            print('{}:{} says {}'.format(*addr, data.decode()))
            
        
    except socket.timeout:
        print("Operação de socket atingiu o timeout.")
    except Exception as e:
        print(f"Ocorreu um erro: {e}")

def main():
    host = sys.argv[1]
    port = int(sys.argv[2])
    message = sys.argv[3]
    print("tentando conectar to {}:{}".format(host, port))
    server_addr = (host, port)
    udp_client(server_addr, message)
    print("conectado")

if __name__ == "__main__":
    main()