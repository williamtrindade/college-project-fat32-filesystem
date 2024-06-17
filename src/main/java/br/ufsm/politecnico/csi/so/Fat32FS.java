package br.ufsm.politecnico.csi.so;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Fat32FS implements FileSystem {
    private final Disco disco;

    private final int[] fat = new int[16*1024];

    public Fat32FS() throws IOException {
        this.disco = new Disco();
        if (this.disco.init()) {
            leFat();
            leDiretorio();
        } else {
            criaFat();
            escreveFat();
        }

    }

    private void criaFat() {
        for (int i = 2; i < fat.length; i++) {
            fat[i] = -1;
        }
    }

    private void escreveFat() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(64*1024);
        for (int f : fat) {
            bb.putInt(f);
        }
        byte[] blocoFat = bb.array();
        disco.escreveBloco(BLOCO_FAT, blocoFat);
    }

    private static final int BLOCO_FAT = 1;
    private void leFat() throws IOException {
        byte[] blocoFat = disco.leBloco(BLOCO_FAT);
        ByteBuffer bb = ByteBuffer.wrap(blocoFat);
        for (int i = 0; i < 16*1024; i++) {
            fat[i] = bb.getInt();
        }
    }

    public void create(String fileName, byte[] data) {
        // Verifica se há espaço suficiente
        if (data.length > freeSpace()) {
            System.out.println("Não há espaço suficiente.");
            return;
        }

        // Divide o nome do arquivo em nome e extensão
        String[] partes = fileName.split("\\.");
        String nome = partes[0];
        String extensao = partes.length > 1 ? partes[1] : "";

        // Encontra blocos livres necessários
        int numBlocos = (int) Math.ceil(data.length / (double) Disco.TAM_BLOCO);
        int blocoInicial = encontraBlocoLivre();
        if (blocoInicial == -1) {
            System.out.println("Não há blocos livres.");
            return;
        }

        // Aloca blocos na FAT
        int blocoAtual = blocoInicial;
        for (int i = 0; i < numBlocos; i++) {
            int proxBloco = (i == numBlocos - 1) ? -1 : encontraBlocoLivre();
            fat[blocoAtual] = proxBloco;
            blocoAtual = proxBloco;
        }

        // Escreve os dados nos blocos
        try {
            for (int i = 0; i < numBlocos; i++) {
                byte[] buffer = new byte[Disco.TAM_BLOCO];
                int start = i * Disco.TAM_BLOCO;
                int length = Math.min(data.length - start, Disco.TAM_BLOCO);
                System.arraycopy(data, start, buffer, 0, length);
                disco.escreveBloco(blocoInicial + i, buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Cria a entrada do diretório e escreve no disco
        EntradaArquivoDiretorio entrada = new EntradaArquivoDiretorio(nome, extensao, data.length, blocoInicial);
        diretorioRaiz.add(entrada);
        try {
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int encontraBlocoLivre() {
        for (int i = 2; i < fat.length; i++) {
            if (fat[i] == -1) {
                return i;
            }
        }
        return -1; // Retorna -1 se não encontrar bloco livre
    }

    public void append(String fileName, byte[] data) {
        EntradaArquivoDiretorio entrada = encontraArquivo(fileName);
        if (entrada == null) {
            System.out.println("Arquivo não encontrado.");
            return;
        }

        // Calcula os novos blocos necessários
        int novosBlocos = (int) Math.ceil((data.length + entrada.tamanho) / (double) Disco.TAM_BLOCO);
        int blocoAtual = entrada.blocoInicial;
        while (fat[blocoAtual] != -1) {
            blocoAtual = fat[blocoAtual];
        }

        // Aloca novos blocos na FAT
        int blocoInicioNovo = encontraBlocoLivre();
        if (blocoInicioNovo == -1) {
            System.out.println("Não há blocos livres.");
            return;
        }
        fat[blocoAtual] = blocoInicioNovo;

        // Escreve os dados nos novos blocos
        try {
            for (int i = 0; i < novosBlocos; i++) {
                byte[] buffer = new byte[Disco.TAM_BLOCO];
                int start = i * Disco.TAM_BLOCO;
                int length = Math.min(data.length - start, Disco.TAM_BLOCO);
                System.arraycopy(data, start, buffer, 0, length);
                disco.escreveBloco(blocoInicioNovo + i, buffer);
                fat[blocoInicioNovo + i] = -1;
            }
            entrada.tamanho += data.length;
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] read(String fileName, int offset, int limit) {
        EntradaArquivoDiretorio entrada = encontraArquivo(fileName);
        if (entrada == null) {
            System.out.println("Arquivo não encontrado.");
            return null;
        }

        if (offset >= entrada.tamanho) {
            System.out.println("Offset fora dos limites do arquivo.");
            return null;
        }

        int bytesToRead = limit == -1 ? entrada.tamanho - offset : Math.min(limit, entrada.tamanho - offset);
        byte[] buffer = new byte[bytesToRead];
        int bytesRead = 0;

        try {
            int blocoAtual = entrada.blocoInicial;
            while (offset >= Disco.TAM_BLOCO) {
                blocoAtual = fat[blocoAtual];
                offset -= Disco.TAM_BLOCO;
            }
            while (bytesRead < bytesToRead) {
                byte[] bloco = disco.leBloco(blocoAtual);
                int bytesToCopy = Math.min(bytesToRead - bytesRead, Disco.TAM_BLOCO - offset);
                System.arraycopy(bloco, offset, buffer, bytesRead, bytesToCopy);
                bytesRead += bytesToCopy;
                blocoAtual = fat[blocoAtual];
                offset = 0; // Reset offset after first block
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }


    @Override
    public void remove(String fileName) {
        EntradaArquivoDiretorio entrada = encontraArquivo(fileName);
        if (entrada == null) {
            System.out.println("Arquivo não encontrado.");
            return;
        }

        int blocoAtual = entrada.blocoInicial;
        while (blocoAtual != -1) {
            int proxBloco = fat[blocoAtual];
            fat[blocoAtual] = -1;
            blocoAtual = proxBloco;
        }

        diretorioRaiz.remove(entrada);
        try {
            escreveDiretorio();
            escreveFat();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int freeSpace() {
        int freeBlocks = 0;
        for (int i = 2; i < fat.length; i++) {
            if (fat[i] == -1) {
                freeBlocks++;
            }
        }
        return freeBlocks * Disco.TAM_BLOCO;
    }

    private static class EntradaArquivoDiretorio {
        private String nomeArquivo;
        private String extensao;
        private int tamanho;
        private final int blocoInicial;

        public EntradaArquivoDiretorio(String nomeArquivo,
                                       String extensao,
                                       int tamanho,
                                       int blocoInicial) {
            this.nomeArquivo = nomeArquivo;
            if (this.nomeArquivo.length() > 8) {
                this.nomeArquivo = nomeArquivo.substring(0, 8);
            } else if (this.nomeArquivo.length() < 8) {
                do {
                    this.nomeArquivo += " ";
                } while (this.nomeArquivo.length() < 8);
            }
            this.extensao = extensao;
            if (this.extensao.length() > 3) {
                this.extensao = extensao.substring(0, 3);
            } else if (this.extensao.length() < 3) {
                do {
                    this.extensao += " ";
                } while (this.extensao.length() < 3);
            }
            this.tamanho = tamanho;
            this.blocoInicial = blocoInicial;
            if (blocoInicial < 2 || blocoInicial >= Disco.NUM_BLOCOS) {
                throw new IllegalArgumentException("numero de bloco invalido");
            }
        }

        public byte[] toByteArray(ByteBuffer bb) {
            bb.put(nomeArquivo.getBytes(StandardCharsets.ISO_8859_1));
            bb.put(extensao.getBytes(StandardCharsets.ISO_8859_1));
            bb.putInt(tamanho);
            bb.putInt(blocoInicial);
            return bb.array();
        }

        private static int intFromBytes(byte[] data, int index) {
            ByteBuffer bb = ByteBuffer.wrap(data);
            return bb.getInt(index);
        }

        public static EntradaArquivoDiretorio fromBytes(byte[] bytes) {
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);
            System.out.println(nome);
            System.out.println(extensao);
            System.out.println(tamanho);
            System.out.println(blocoInicial);
            return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
        }

        public static EntradaArquivoDiretorio fromStream(InputStream inputStream) throws IOException {
            byte[] bytes = new byte[19];
            inputStream.read(bytes);
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);
            System.out.println(nome);
            System.out.println(extensao);
            System.out.println(tamanho);
            System.out.println(blocoInicial);
            return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
        }

    }

    private static final int BLOCO_DIRETORIO = 0;
    private final List<EntradaArquivoDiretorio> diretorioRaiz = new ArrayList<>();

    private void leDiretorio() throws IOException {
        byte[] dirBytes = disco.leBloco(BLOCO_DIRETORIO);
        ByteArrayInputStream bin = new ByteArrayInputStream(dirBytes);
        EntradaArquivoDiretorio entrada = null;
        do {
            entrada = EntradaArquivoDiretorio.fromStream(bin);
            if (entrada.tamanho > 0) {
                diretorioRaiz.add(entrada);
            }
        } while(entrada.tamanho > 0);

    }

    private void escreveDiretorio() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Disco.TAM_BLOCO);
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            entrada.toByteArray(bb);
        }
        disco.escreveBloco(BLOCO_DIRETORIO, bb.array());
    }

    private EntradaArquivoDiretorio encontraArquivo(String fileName) {
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            if ((entrada.nomeArquivo.trim() + "." + entrada.extensao.trim()).equals(fileName)) {
                return entrada;
            }
        }
        return null;
    }

}
