# Documento do Game de Duelo - Projeto de Redes de Computadores - UFC Quixadá - Juan Pimentel
- Versão: 1.0
- Data de Início: 19/03/2024
- Descrição: 
  - Este documento descreve o projeto de um jogo de duelo em rede, que será desenvolvido na disciplina de Redes de Computadores.
- Objetivo: 
  - O objetivo do projeto é desenvolver um jogo de duelo em rede, onde dois jogadores poderão se enfrentar em uma batalha de espada e/ou outras armas.
- Materiais:
  - Para o desenvolvimento do projeto, será necessário um computador com sistema operacional Linux, a linguagem de programação Python e a biblioteca Pygame.
## O jogo terá as seguintes requisitos para estar completo: 
  - R1 - Conexão entre dois jogadores por meio de sockets p2p.
  - R2 - Movimentação dos jogadores.
  - R3 - Ataque dos jogadores.
  - R4 - Defesa dos jogadores.
  - R5 - Contagem de vida.
  - R6 - Contagem de tempo.
  - R7 - Tela de vitória e derrota.
  - R8 - Tela de início e fim de jogo.
  - R9 - Autenticação.
  - R10 - Ranking.
  - R11 - Busca de jogador por nickname.
  - R12 - multithreading para manter os jogadores conectados com o servidor.
  - R13 - Tela de espera da luta.

## Restrições: 
O projeto deverá ser desenvolvido em Python e a biblioteca Pygame <br/>
O Projeto deve usar sprites livres de direitos autorais <br/>
O Projeto deve ser executado em um sistema operacional Linux. <br/>

## Cronograma: O projeto será desenvolvido ao longo do semestre, com as seguintes etapas:
- E1 - Estudo de sockets em Python.
  - E1.1 - Fazer aplicação de chat em rede com autenticação.
  - E1.2 - Fazer aplicação de chat que usa comandos.
  - E1.3 - Fazer aplicação de chat entre pares.
  - E1.4 - Fazer aplicação de chat entre pares que segue comandos.
  - E1.5 - Fazer aplicação de chat entre pares e geral que segue comandos e retorna resultado do chat pro servidor e a persistencia.
- E2 - Estudo da biblioteca Pygame.
  - E2.1 - Fazer um jogo simples com a biblioteca Pygame.
  - E2.2 - Conectar a funcionalidade do jogo com os comandos de cliente e servidor.
  - E2.3 - Fazer funcionar a lógica duelo em rede e autenticação.
  - E2.4 - Fazer funcionar a lógica de resultado e ranking.
- E3 - Desenvolvimento do jogo.
  - E3.0 - Desenvolver a lógica do jogo e implementar a persistência.
  - E3.1 - Desenvolver a tela de início.
  - E3.2 - Desenvolver a tela de espera.
  - E3.3 - Desenvolver a tela de luta.
  - E3.5 - Desenvolver a tela de ranking.
  - E3.6 - Desenvolver a tela de busca.
  - E3.7 - Desenvolver a tela de autenticação.
  - E3.8 - Desenvolver a tela de fim de jogo e resultado.
- E4 - Testes.
- E5 - Entrega.

## Equipe: 
O projeto será desenvolvido por Juan Pimentel, estudante de Engenharia de Software da UFC Quixadá.
## Referências: 
- https://www.pygame.org/docs/
- https://docs.python.org/3/library/socket.html
- https://docs.python.org/3/library/sqlite3.html#sqlite3-reference
- https://docs.python.org/pt-br/3/library/threading.html#module-threading
- https://sanderfrenken.github.io/Universal-LPC-Spritesheet-Character-Generator/#?body=Body_color_light&head=Human_male_light&weapon=Longsword_longsword&clothes=Longsleeve_blue