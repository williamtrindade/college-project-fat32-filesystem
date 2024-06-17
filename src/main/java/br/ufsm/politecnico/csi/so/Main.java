package br.ufsm.politecnico.csi.so;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Iniciando filesystem...");
        Fat32FS fat32FS = new Fat32FS();

       // byte[] data = disco.leBloco(0);
        //System.out.println(new String(data, StandardCharsets.UTF_8));
        byte[] arqBytes = Files.readAllBytes(Paths.get("arq.txt"));
        fat32FS.create("arq.txt", arqBytes);
    }
}