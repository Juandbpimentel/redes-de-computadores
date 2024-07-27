import socket
import struct
import sys
import threading
import time


def recieveMessage(soc: socket.socket):
    print("Aguardando mensagens do peer...")
    while True:
        try:
            soc.settimeout(30)
            data, addr = soc.recvfrom(1024)
            if data != None:
                print("Mensagem recebida!")
                print(f'{addr} disse: {data.decode()}')
                if data.decode() == 'exit':
                    print('Encerrando conexão...')
                    soc.close()
                    break
        except socket.timeout:
            print('Nenhuma mensagem recebida após muito tempo, encerrando recepção de mensagens.')
            soc.close()
            break
        except Exception as e:
            print(f'Ocorreu um erro: {e}')
            soc.close()
            break


def udp_client_connect(reciever_soc,server):
    try:
        reciever_soc.sendto(b'', server)

        print('Aguardando resposta do servidor com peer...')
        data, _ = reciever_soc.recvfrom(1024)
        data = data.decode().split(',')
        dataAddr = (data[0].replace('\'','').replace('(','').replace(' ',''))
        dataSocket = int(data[1].replace(' ','').replace(')',''))
        peer = (dataAddr, dataSocket)
        return peer
    except socket.timeout:
        print('Operação de socket atingiu o timeout.')
    except Exception as e:
        print(f'Ocorreu um erro: {e}')


def main():
    host = sys.argv[1]
    port = int(sys.argv[2])
    
    server_addr = (host, port)
    reciever_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sender_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    peer = udp_client_connect(reciever_soc,server_addr)
    
    print('Conectado ao peer: ', peer)
    print('Criando thread para receber mensagens\n\n')
    thread = threading.Thread(target=recieveMessage, args=(reciever_soc,))
    thread.start()
    time.sleep(1)

    print(f'Enviando mensagens para {peer}...')
    for i in range(5):
        sender_soc.sendto(str(f"Datagrama {i+1}").encode(), peer)
        time.sleep(1)
    
    print(f'\n\nSocket de envio: {sender_soc.getsockname()}\n'+
          f'Socket de recebimento: {reciever_soc.getsockname()}')
    sender_soc.sendto('exit'.encode(), peer)
    
    sender_soc.close()
    
    thread.join()
    print("Main encerrada")
    return


if __name__ == "__main__":
    main()
