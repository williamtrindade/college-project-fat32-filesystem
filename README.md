# college-project-fat32-filesystem
Operational Systems project from UFSM systems for internet course

Desenvolver uma classe em Java que implemente um Sistema de Arquivos FAT 32 através da
interface a seguir:

Este sistema, de acordo com a interface proposta, fará a operação de um sistema de arquivos
FAT32 armazenando arquivos em um único arquivo do sistema, como se este arquivo fosse
uma unidade de armazenamento secundária. Através das chamadas à interface será permitido
que os usuários operem o sistema de arquivos.

Observações:
- [ ] Deverá ser desenvolvido em linguagem Java utilizando a classe RandomAccessFile e a
chamada seek(pos) para busca e leitura/gravação de blocos inteiros;
- [ ] O sistema de arquivos será de diretório único;
- [ ] Os blocos serão de 64KB;
- [ ] O tamanho do arquivo para armazenamento poderá ser definido na inicialização (caso
o arquivo seja novo), sempre em relação a um determinado número de blocos;
- [ ] O primeiro bloco será reservado para as informações do diretório (único), dentre as
  quais:
  - [ ] Nome do arquivo: tamanho fixo de 8 caracteres e três para a extensão.
  - [ ] Tamanho total do arquivo: inteiro de 32 bits.
  - [ ] Tamanho total do arquivo: inteiro de 32 bits.

- [ ] Os próximos blocos serão usados para o armazenamento da FAT (calcular a
  quantidade de blocos necessários de acordo com o tamanho do arquivo);
- [ ] Operações inválidas geram exceções como por exemplo: tentar ler um arquivo além
  do seu tamanho, tentar gravar mais dados do que o espaço livre permite, etc.
- [ ] Deverão ser desenvolvidos casos de teste para cada uma das chamadas
  implementadas;
- [ ] O trabalho poderá ser realizado em duplas;
- [ ] A entrega do código fonte deverá ser feita pelo Moodle até o dia 15/05/24. A entrega
  só será considerada mediante apresentação ao professor;