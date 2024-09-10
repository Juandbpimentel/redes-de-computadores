# Game de Duelo em Tempo Real com Sockets

Este é um jogo multiplayer que utiliza sockets para estabelecer a conexão entre os jogadores. 

## Como Jogar

1. Clone este repositório em sua máquina local.
2. Certifique-se de ter a JRE versão 11 instalada em seu sistema.
3. Execute o arquivo app.exe para iniciar o jogo.
4. Caso tenha interesse, recompile o jogo com as configurações desejadas no arquivo de variaveis de ambiente da godot.
5. Divirta-se jogando!
O jogo é desenvolvido na Godot e requer a JRE versão 11 para habilitar o UPnP utilizando a seguinte ferramenta [PortMapper](https://github.com/kaklakariada/portmapper) . Além disso, se você deseja compilar o jogo em vez de usar o executável, é necessário configurar as variáveis de ambiente adequadas.

## Recursos

- O jogo é um game em tempo real de duelo.
- Os jogadores podem desafiar outros jogadores para um duelo ou jogar escolher um adversário aleatório na busca de duelo.

## Contribuição

Sinta-se à vontade para contribuir com melhorias para este jogo. Basta seguir as etapas abaixo:

1. Faça um fork deste repositório.
2. Crie uma branch para suas alterações: `git checkout -b minha-feature`.
3. Faça as alterações desejadas e faça commit delas: `git commit -m 'Minha nova feature'`.
4. Envie suas alterações para o repositório remoto: `git push origin minha-feature`.
5. Abra um pull request para que suas alterações sejam revisadas e incorporadas ao jogo.

## Licença

Este jogo é distribuído sob a licença GPL-3.0. Consulte o arquivo `LICENSE` para obter mais informações.

## Server

O servidor é um aplicativo em Java que utiliza Gradle e Spring para sua implementação. Ele é responsável por gerenciar a conexão entre os jogadores e manter a sincronização do jogo. Certifique-se de ter o Gradle e o Spring instalados em seu sistema antes de executar o servidor.

Para iniciar o servidor, siga as etapas abaixo:

1. Abra o terminal e navegue até o diretório do server no projeto.
2. Configure o arquivo `application.properties` conforme necessário (Para configurar o ip e porta do servidor).
2. Execute o comando `gradle build` para compilar o projeto.
3. Em seguida, execute o comando `gradle bootRun` para iniciar o servidor.

Certifique-se de que o servidor esteja em execução antes de os jogadores se conectarem.
