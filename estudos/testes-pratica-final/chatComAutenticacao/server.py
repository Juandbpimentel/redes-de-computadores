import errno
import socket
import threading
import sqlite3

con = sqlite3.connect("game_data.db", check_same_thread=False)
cursor = con.cursor()
cursor.execute(
    """CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, Victories INTEGER)"""
)

HOST = "192.168.0.100"  # Endereço do servidor
PORT = 12345  # Porta para conexão
BUFFER_SIZE = 1024

clients = {}
usernames = {}


def broadcast(message, sender_addr):
    for client, conn in clients.items():
        if client != sender_addr:
            try:
                conn.sendall(message.encode("utf-8"))
            except Exception as e:
                print(f"Erro ao enviar mensagem para {client}: {e}")
                del clients[client]
                del usernames[client]


def handle_login(client_socket, addr, username) -> bool:
    # Verifica se o usuário está no banco de dados
    cursor.execute(f'SELECT * FROM users WHERE username = "{username}"')
    result_user = cursor.fetchone()

    if not result_user:

        client_socket.sendall(
            "Você quer criar uma conta? digite y para sim e n para não:  ".encode(
                "utf-8"
            )
        )
        create_account = client_socket.recv(BUFFER_SIZE).decode("utf-8").strip()
        if create_account == "y":
            client_socket.sendall("Digite sua senha: ".encode("utf-8"))
            password = client_socket.recv(BUFFER_SIZE).decode("utf-8").strip()
            cursor.execute(
                f'INSERT INTO users (username, password, Victories) VALUES ("{username}", "{password}", 0)'
            )
            con.commit()
            print(f"Usuário criado por [ {addr[0]}:{addr[1]}]: {username}")
            client_socket.sendall(f"Bem vindo {username}!".encode("utf-8"))
            clients[addr] = client_socket
            usernames[addr] = username
            return True
        else:
            client_socket.sendall("Ok, valeu :) !".encode("utf-8"))
            client_socket.close()
            print(f"Cliete Desconectado: [ {addr[0]}:{addr[1]} ]")
            return False
    else:

        for user in usernames:
            if usernames[user] == username:
                client_socket.sendall(
                    "Alguém já está conectado na sua conta.".encode("utf-8")
                )
                client_socket.close()
                print(f"Cliete Desconectado: [ {addr[0]}:{addr[1]} ]")
                return False

        for i in range(3):

            if i == 0:
                client_socket.sendall("Digite sua senha: ".encode("utf-8"))
            else:
                client_socket.sendall(
                    "Senha incorreta, digite novamente: ".encode("utf-8")
                )

            password = client_socket.recv(BUFFER_SIZE).decode("utf-8").strip()

            if password == result_user[1]:
                break
            if password != result_user[1] and i == 2:
                client_socket.sendall(
                    "Senha incorreta, fechando conexão!".encode("utf-8")
                )
                client_socket.close()
                print(f"Cliete Desconectado: [ {addr[0]}:{addr[1]} ]")
                return False

        clients[addr] = client_socket
        usernames[addr] = username
        return True


def handle_client(client_socket, addr):
    try:
        # Solicita o nome de usuário ao cliente
        client_socket.sendall("Digite seu nome de usuário: ".encode("utf-8"))
        username = client_socket.recv(BUFFER_SIZE).decode("utf-8").strip()

        if not handle_login(client_socket, addr, username):
            del clients[addr]
            del usernames[addr]
            return

        print(f"Login Realizado: [ {username} | {addr[0]}:{addr[1]} ]")
        client_socket.sendall(f"Bem vindo {username}!".encode("utf-8"))
        broadcast(f"{username} entrou no chat!", addr)
        while True:
            try:
                data = client_socket.recv(BUFFER_SIZE)
                if not data:
                    break
                # Processa os dados recebidos
                message = data.decode("utf-8")
                print(f"Mensagem de {username}: {message}")

                broadcast(f"{username}: {message}", addr)
            except socket.error as e:
                if e.errno == errno.WSAECONNRESET:
                    print(
                        f"Usuário deslogado e desconectado: [ {addr[0]}:{addr[1]} | {username} ]"
                    )
                    broadcast(f"{username} saiu do chat!", addr)
                    break
                else:
                    print(f"Erro ao receber dados do cliente {addr}: {e}")
                    break
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
            print(f"Cliente conectado: {address[0]}:{address[1]}")

            # Cria uma thread para lidar com o cliente
            client_thread = threading.Thread(
                target=handle_client, args=(client_socket, address)
            )
            client_thread.start()
    except Exception as e:
        print(f"Erro no servidor: {e}")
    finally:
        server_socket.close()


if __name__ == "__main__":
    main()
    con.close()
