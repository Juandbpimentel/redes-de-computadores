import socket
from typing import Any
import upnpy
import upnpy.ssdp
import upnpy.ssdp.SSDPDevice

class Network:
    
    @staticmethod
    def getMyIp():
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip

    @staticmethod
    def makeUpnpPortMapping(port:int, protocol:str, description:str) -> upnpy.ssdp.SSDPDevice.SSDPDevice.Service:
        upnp = upnpy.UPnP()
        upnp.discover(2)
        router = upnp.get_igd()
        portMapperService = router.WANIPConn1
        
        internalIp = Network.getMyIp()
        
        portMapperService.AddPortMapping(
            NewRemoteHost='',
            NewExternalPort=port,
            NewProtocol=protocol,
            NewInternalPort=port,
            NewInternalClient=internalIp,
            NewEnabled=1,
            NewPortMappingDescription=description,
            NewLeaseDuration=0)
        return portMapperService

    @staticmethod
    def udp_client_connect(reciever_soc,server) -> Any:
        try:
            portMappingService = Network.makeUpnpPortMapping(reciever_soc.getsockname()[1],'UDP','P2P Port For Juan Pimentel game')
            reciever_soc.sendto(b'', server)

            print('Aguardando resposta do servidor com peer...')
            data, _ = reciever_soc.recvfrom(1024)
            data = data.decode().split(',')
            dataAddr = str(data[0].replace('\'','').replace('(','').replace(' ',''))
            dataSocket = int(data[1].replace(' ','').replace(')',''))
            peer = (dataAddr, dataSocket)
            if peer == None or portMappingService == None:
                raise Exception('Erro ao obter peer ou serviço de mapeamento de portas.')
            return (peer , portMappingService)
        except socket.timeout:
            print('Operação de socket atingiu o timeout.')
            return None
        except Exception as e:
            print(f'Ocorreu um erro: {e}')
            return None