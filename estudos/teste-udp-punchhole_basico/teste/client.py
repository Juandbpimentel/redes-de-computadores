import socket
import threading
import time
from networkTools import network


def recieveMessage(soc: socket.socket):
    print("Aguardando mensagens do peer...")
    while True:
        try:
            soc.settimeout(5)
            data, addr = soc.recvfrom(1024)
            if data != None:
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

def main():
    #server host configuration
    server_addr = ('35.208.9.89', 10001)
    
    #client socket configuration
    reciever_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sender_soc = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    reciever_soc.bind(('', 0))
    sender_soc.bind(('', 0))    
    port = reciever_soc.getsockname()[1]
    
    try:
        peer,portMappingService = network.Network.udp_client_connect(reciever_soc,server_addr)
    except Exception as e:
        print(f'Ocorreu um erro: {e}')
        if portMappingService != None: # type: ignore
            portMappingService.DeletePortMapping(NewRemoteHost='', NewExternalPort=port, NewProtocol='UDP') # type: ignore
        return
    
    if portMappingService == None: # type: ignore
        print('Erro ao obter peer ou serviço de mapeamento de portas.')
        return
    
    if peer == None:
        print('Erro ao obter peer ou serviço de mapeamento de portas.')
        portMappingService.DeletePortMapping(NewRemoteHost='', NewExternalPort=port, NewProtocol='UDP') # type: ignore
        return
    
    
    print('Conectado ao peer: ', peer)
    thread = threading.Thread(target=recieveMessage, args=(reciever_soc,))
    thread.start()
    time.sleep(1)

    for i in range(2):
        sender_soc.sendto(str(f"Datagrama {i+1}").encode(), peer)
        time.sleep(1)
    
    
    sender_soc.sendto('exit'.encode(), peer)
    sender_soc.close()
    thread.join()
    portMappingService.DeletePortMapping(NewRemoteHost='', NewExternalPort=port, NewProtocol='UDP')
    print("Main encerrada")
    return
    """
    """


if __name__ == "__main__":
    main()
