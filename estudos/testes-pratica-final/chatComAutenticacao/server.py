import socket
import threading
import sqlite3

con = sqlite3.connect('game_data.db', check_same_thread=False)
cursor = con.cursor()
cursor.execute('''CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, Victories INTEGER)''')

HOST = '192.168.0.100'  # Endereço do servidor
PORT = 12345        # Porta para conexão
BUFFER_SIZE = 1024

clients = {}
usernames = {}


def broadcast(message, sender_addr):
    for client, conn in clients.items():
        if client != sender_addr:
            try:
                conn.sendall(message.encode('utf-8'))
            except Exception as e:
                print(f"Erro ao enviar mensagem para {client}: {e}")
                del clients[client]
                del usernames[client]


def handle_client(client_socket, addr):
    print(f"[NEW CONNECTION] {addr} connected.")
    try:
        # Solicita o nome de usuário ao cliente
        client_socket.sendall("Digite seu nome de usuário: ".encode('utf-8'))
        username = client_socket.recv(BUFFER_SIZE).decode('utf-8').strip()
        # Verifica se o usuário está no banco de dados
        cursor.execute(f'SELECT * FROM users WHERE username = "{username}"')
        user = cursor.fetchone()
        if not user:
            client_socket.sendall("Você quer criar uma conta? digite y para sim e n para não:  ".encode('utf-8'))
            create_account = client_socket.recv(BUFFER_SIZE).decode('utf-8').strip()
            if create_account == 'y':
                client_socket.sendall("Digite sua senha: ".encode('utf-8'))
                password = client_socket.recv(BUFFER_SIZE).decode('utf-8').strip()
                cursor.execute(f'INSERT INTO users (username, password, Victories) VALUES ("{username}", "{password}", 0)')
                con.commit()
                print(f"[NEW ACCOUNT] {username} created.")
                clients[addr] = client_socket
                usernames[addr] = username
            else:
                client_socket.sendall("Ok, valeu :) !".encode('utf-8'))
                client_socket.close()
                print(f"[DISCONNECTED] {addr} disconnected.")
                return
        else:
            for user in usernames:
                if usernames[user] == username:
                    client_socket.sendall("Alguém já está conectado na sua conta".encode('utf-8'))
                    client_socket.close()
                    print(f"[DISCONNECTED] {addr} disconnected.")
                    return

            client_socket.sendall("Digite sua senha: ".encode('utf-8'))
            password = client_socket.recv(BUFFER_SIZE).decode('utf-8').strip()
            if password != user[1]:
                client_socket.sendall("Senha incorreta.".encode('utf-8'))
                client_socket.close()
                print(f"[DISCONNECTED] {addr} disconnected.")
                return
            clients[addr] = client_socket
            usernames[addr] = username

        print(f"[{addr}] {username} connected.")
        client_socket.sendall(f"Bem vindo {username}!".encode('utf-8'))
        broadcast(f"{username} entrou no chat!".encode('utf-8'), addr)
        while True:
            data = client_socket.recv(BUFFER_SIZE)
            if not data:
                break
            # Processa os dados recebidos
            message = data.decode('utf-8')
            if message == 'close()':
                print(f"[DISCONNECTED] {addr} disconnected.")
                client_socket.close()
                break
            print(f"Mensagem de {username}: {message}")

            broadcast(f"{username}: {message}".encode('utf-8'), addr)
        del clients[addr]  # Remove o cliente da lista após a desconexão
        del usernames[addr]  # Remove o nome de usuário após a desconexão
    except Exception as e:
        print(f"Erro ao lidar com o cliente {addr}: {e}")
    finally:
        client_socket.close()

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

            # Cria uma thread para lidar com o cliente
            client_thread = threading.Thread(target=handle_client, args=(client_socket, address))
            client_thread.start()
    except Exception as e:
        print(f"Erro no servidor: {e}")
    finally:
        server_socket.close()

if __name__ == '__main__':
    main()
    con.close()