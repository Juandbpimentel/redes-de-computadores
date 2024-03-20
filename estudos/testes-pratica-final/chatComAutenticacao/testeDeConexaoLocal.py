import socket

HOST = '192.168.0.100'  # Endereço do servidor
PORT = 12345        # Porta para conexão
BUFFER_SIZE = 1024  # Tamanho do buffer de dados

def main():
    # Criação do socket TCP/IP
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        # Vincula o socket ao endereço e porta especificados
        server_socket.bind((HOST, PORT))
        server_socket.listen(5)
        print(f"Servidor escutando em {HOST}:{PORT}")

        while True:
            # Aceita a conexão do cliente
            client_socket, address = server_socket.accept()
            print(f"Cliente conectado: {address}")
            client_socket.sendall("Hello World!".encode('utf-8'))
            client_socket.close()
    except Exception as e:
        print(f"Erro: {e}")

if __name__ == '__main__':
    main()