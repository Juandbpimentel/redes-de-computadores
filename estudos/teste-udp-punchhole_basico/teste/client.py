import socket
import struct
import sys
import threading
import time


def addr_to_bytes(addr):
    return socket.inet_aton(addr[0]) + struct.pack('H', addr[1])


def bytes_to_addr(addr):
    return socket.inet_ntoa(addr[:4]), struct.unpack('H', addr[4:])[0]


def recieveMessage(soc: socket.socket):
    print("Aguardando resposta do peer...")
    while True:
        data, addr = soc.recvfrom(1024)
        if data is not None:
            print("Mensagem recebida!")
            print(f'{addr} disse: {data.decode()}')
            break


def udp_client(server, message):
    reciever_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        print('Enviando pacote vazio para o servidor...')
        reciever_soc.sendto(b'', server)

        print('Aguardando resposta do servidor...')
        data, _ = reciever_soc.recvfrom(1024)
        peer = bytes_to_addr(data)

        print(peer[0])
        print(peer[1])

        print('Conectado ao peer: ', peer)
        print('Criando thread para receber mensagens')
        recieveMessageThread = threading.Thread(target=recieveMessage, args=(reciever_soc,))
        recieveMessageThread.start()
        time.sleep(3)

        sender_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        print('Enviando uma mensagem para o peer...')
        sender_soc.sendto(message.encode(), peer)

        print('Mensagem enviada!')
        reciever_soc.close()
        sender_soc.close()
        recieveMessageThread.join()
    except socket.timeout:
        print('Operação de socket atingiu o timeout.')
    except Exception as e:
        print(f'Ocorreu um erro: {e}')


def main():
    print("Iniciando whatsapp")
    host = sys.argv[1]
    port = int(sys.argv[2])
    message: str = input("Digite a mensagem que deseja enviar para o seu parceiro: ")
    print(f'tentando conectar to {host}:{port}')
    server_addr = (host, port)
    udp_client(server_addr, message)
    print("encerrado")
    return


if __name__ == "__main__":
    main()
