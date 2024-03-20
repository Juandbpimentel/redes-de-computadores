import socket
import threading

HOST = '192.168.0.100'  # Endereço do servidor
PORT = 12345  # Porta para conexão
BUFFER_SIZE = 1024


def receive_messages(client_socket):
    while True:
        try:
            data = client_socket.recv(BUFFER_SIZE)
            if not data:
                break
            print(data.decode('utf-8'))
        except Exception as e:
            print(f"Erro ao receber dados do servidor: {e}")
            break

def fazer_login(client_socket):
    try:
        username = input(client_socket.recv(BUFFER_SIZE).decode('utf-8'))
        client_socket.sendall(username.encode('utf-8'))
        password = input(client_socket.recv(BUFFER_SIZE).decode('utf-8'))
        client_socket.sendall(password.encode('utf-8'))
        print(client_socket.recv(BUFFER_SIZE).decode('utf-8'))
    except Exception as e:
        print(f"Erro ao conectar ao servidor: {e}")

def main():
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    try:
        client_socket.connect((HOST, PORT))
        print("Conectado ao servidor")

        receive_thread = threading.Thread(target=receive_messages, args=(client_socket,))
        receive_thread.start()

        while True:
            message = input(">> ")
            if message.lower() == 'close()':
                client_socket.close()
                break
            client_socket.sendall(message.encode('utf-8'))
    except Exception as e:
        print(f"Erro ao conectar ao servidor: {e}")
    finally:
        client_socket.close()

if __name__ == '__main__':
    main()
