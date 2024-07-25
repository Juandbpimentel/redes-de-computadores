import socket
import struct
import sys
import threading
import time

soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])

def bytes_to_addr(addr):
    return (socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0])

def recieveMessage(soc: socket):
    print("Aguardando resposta do peer...")
    data, addr = soc.recvfrom(1024)
    print(f'{addr} disse: {data.decode()}')

def udp_client(server, message):
    try:
        print("Enviando pacote vazio para o servidor...")
        soc.sendto(b'', server)
        
        print("Aguardando resposta do servidor...")
        data, addr = soc.recvfrom(6)
        peer = bytes_to_addr(data)
        recieveMessageThread = threading.Thread(target=recieveMessage, args=(soc,))
        recieveMessageThread.start()
        print('conectado ao peer:', *peer)
        time.sleep(3)
        print('Enviando uma mensagem para o peer...')
        soc.sendto(message.encode(), peer)
        
    except socket.timeout:
        print("Operação de socket atingiu o timeout.")
    except Exception as e:
        print(f"Ocorreu um erro: {e}")

def main():
    print("Iniciando whatsapp")
    host = sys.argv[1]
    port = int(sys.argv[2])
    message:str = input("Digite a mensagem que deseja enviar para o seu parceiro: ")
    print("tentando conectar to {}:{}".format(host, port))
    server_addr = (host, port)
    udp_client(server_addr, message)
    print("encerrado")
    soc.close()
    return

if __name__ == "__main__":
    main()
