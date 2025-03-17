package com.mycompany.practica1.onedrive;

import java.net.ServerSocket;
import java.io.*;
import java.net.Socket;
import java.net.StandardSocketOptions;

public class Servidor {
    public static void main(String[] args) {
        try {
            ServerSocket sControl = new ServerSocket(1234);
            ServerSocket sDatos = new ServerSocket(1235);

            // Configuración de opciones del socket
            sControl.setOption(StandardSocketOptions.SO_REUSEADDR, true);// Permite reutilizar el puerto
            sDatos.setOption(StandardSocketOptions.SO_REUSEADDR, true);// Permite reutilizar el puerto
            System.out.println("Servidor de control iniciado en el puerto: " + sControl.getLocalPort());
            System.out.println("Servidor de datos iniciado en el puerto: " + sDatos.getLocalPort());

            for(;;){
                // Aceptar conexión del cliente en el socket de control
                Socket socketControl = sControl.accept();
                System.out.println("Cliente conectado: " + socketControl.getInetAddress());

                // Aceptar conexión del cliente en el socket de datos
                Socket socketDatos = sDatos.accept();
                System.out.println("Conexión de datos establecida");

                // Procesar comandos del cliente
                procesarComandos(socketControl, socketDatos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void procesarComandos(Socket socketControl, Socket socketDatos) {
        try{
            BufferedReader inControl = new BufferedReader(new InputStreamReader(socketControl.getInputStream())); //Para recibir datos
            PrintWriter outControl = new PrintWriter(socketControl.getOutputStream(), true); //Para enviar datos
            BufferedReader inDatos = new BufferedReader(new InputStreamReader(socketDatos.getInputStream())); //Para recibir datos
            PrintWriter outDatos = new PrintWriter(socketDatos.getOutputStream(), true); //Para enviar datos

            String comando;
            while ((comando = inControl.readLine()) != null) {
                System.out.println("Comando recibido: " + comando);

                // Procesar el comando
                switch (comando) {
                    case "LIST":
                        listarArchivos(outDatos);
                        break;
                    case "RETR":
                        enviarArchivo(inControl.readLine(), outDatos);
                        break;
                    case "STOR":
                        recibirArchivo(inControl.readLine(), inDatos);
                        break;
                    default:
                        outControl.println("Comando no reconocido");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void listarArchivos(PrintWriter outDatos) {
        File directorio = new File("C:\\Users\\crist\\OneDrive\\Escritorio\\xd"); //ruta
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                outDatos.println(archivo.getName());
            }
        }
    }

    private static void enviarArchivo(String nombreArchivo, PrintWriter outDatos) {
        //enviar un archivo
    }

    private static void recibirArchivo(String nombreArchivo, BufferedReader inDatos) {
        //recibir un archivo
    }
}

