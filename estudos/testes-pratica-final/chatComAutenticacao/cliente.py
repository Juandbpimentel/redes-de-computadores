import socket
import sys
import threading
import termios, sys

def flush_input():
    termios.tcflush(sys.stdin, termios.TCIOFLUSH)


class thread_with_trace(threading.Thread):
    def __init__(self, *args, **keywords):
        threading.Thread.__init__(self, *args, **keywords)
        self.__run_backup = None
        self.killed = False

    def start(self):
        self.__run_backup = self.run
        self.run = self.__run
        threading.Thread.start(self)

    def __run(self):
        sys.settrace(self.globaltrace)
        self.__run_backup()
        self.run = self.__run_backup

    def globaltrace(self, frame, event, arg):
        if event == "call":
            return self.localtrace
        else:
            return None

    def localtrace(self, frame, event, arg):
        if self.killed:
            if event == "line":
                raise SystemExit()
        return self.localtrace

    def kill(self):
        self.killed = True


#HOST = "192.168.0.17"  # Endereço do servidor
#PORT = 12345  # Porta para conexão
BUFFER_SIZE = 1024


def receive_messages(client_socket):
    while True:
        try:
            data = client_socket.recv(BUFFER_SIZE)
            if not data:
                break
            flush_input()
            print("")
            print('>> '+data.decode("utf-8"),end='\n')
            
        except socket.error as e:
            print(f"Erro ao receber dados do servidor <Thread>: {e}")
            break


def fazer_login(client_socket) -> bool:
    try:
        rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
        if rcvd_message == "Digite seu nome de usuário: ":
            print(rcvd_message, end="")
            username = input()
            client_socket.sendall(username.encode("utf-8"))
        else:
            return False

        rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
        match rcvd_message:
            case "Você quer criar uma conta? digite y para sim e n para não:  ":
                print(rcvd_message, end="")
                opt = "y" if input() == "y" else "n"
                match opt:
                    case "y" | "Y":
                        client_socket.sendall("y".encode("utf-8"))
                        while True:
                            rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
                            if rcvd_message == "Digite sua senha: ":
                                print(rcvd_message, end="")
                                password = input()
                                client_socket.sendall(password.encode("utf-8"))
                                print(client_socket.recv(BUFFER_SIZE).decode("utf-8"))
                                return True
                            else:
                                print(rcvd_message)
                                return False
                    case "n" | "N":
                        client_socket.sendall("n".encode("utf-8"))
                        rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
                        print(rcvd_message)
                        return False
                    case _:
                        client_socket.sendall("n".encode("utf-8"))
                        rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
                        print(rcvd_message)
                        return False
            case "Digite sua senha: ":
                i = 0
                while True:
                    if i != 0:
                        rcvd_message = client_socket.recv(BUFFER_SIZE).decode("utf-8")
                    i += 1
                    if rcvd_message == "Digite sua senha: ":
                        print(rcvd_message, end="")
                        password = input()
                        client_socket.sendall(password.encode("utf-8"))
                    elif rcvd_message == "Senha incorreta, digite novamente: ":
                        print(rcvd_message)
                        password = input()
                        client_socket.sendall(password.encode("utf-8"))
                    elif rcvd_message == f"Bem vindo {username}!":
                        print(rcvd_message,end="\n")
                        return True
                    else:
                        return False
            case "Alguém já está conectado na sua conta.":
                print(rcvd_message)
                return False
    except Exception as e:
        print(f"Erro ao conectar ao servidor <Login>: {e}")
        return False


def main():
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        HOST = input("Digite o endereço do servidor: ")
        PORT = int(input("Digite a porta do servidor: "))
        client_socket.connect((HOST, PORT))
        print("Conectando ao servidor")
        if not fazer_login(client_socket):
            client_socket.close()
            return
        receive_thread = thread_with_trace(target=receive_messages, args=(client_socket,))
        receive_thread.start()

        while True:
            message = input()
            if message.lower() == "close()":
                client_socket.sendall(message.encode("utf-8"))                    
                receive_thread.kill()
                client_socket.close()
                break
            client_socket.sendall(message.encode("utf-8"))
    except Exception as e:
        print(f"Erro ao conectar ao servidor: {e}")
    finally:
        client_socket.close()


if __name__ == "__main__":
    main()
    print("Desconectado do servidor")
