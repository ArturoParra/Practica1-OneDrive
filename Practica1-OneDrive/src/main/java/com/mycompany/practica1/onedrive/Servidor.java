package com.mycompany.practica1.onedrive;

import java.net.ServerSocket;
import java.io.*;
import java.net.Socket;
import javax.swing.JFileChooser;

public class Servidor {
    public static void main(String[] args) {
        try {
            ServerSocket sControl = new ServerSocket(1234);
            ServerSocket sDatos = new ServerSocket(1235);

            // Configuración de opciones del socket
            //sControl.setOption(StandardSocketOptions.SO_REUSEADDR, true);// Permite reutilizar el puerto
            sControl.setReuseAddress(true);
            //sDatos.setOption(StandardSocketOptions.SO_REUSEADDR, true);// Permite reutilizar el puerto
            sDatos.setReuseAddress(true);
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
                    case "lss":
                        File directorio = new File("./dataserver");
                        listarArchivos(directorio, outDatos);
                        outControl.println("Listando archivos...");
                        outDatos.println("END");// Marca de fin de la comunicación
                        break;
                    case "dwld":
                        outControl.println("Descargando archivo...");
                        enviarArchivo(inControl.readLine(), outDatos);
                        outDatos.println("END");
                        break;
                    case "upld":
                        outControl.println("Subiendo archivo...");
                        recibirArchivo(inControl.readLine(), inDatos);
                        outDatos.println("END");
                        break;
                    case "mkfiles":
                        outControl.println("Creando archivo...");
                        break;
                    case "rmfiles":
                        outControl.println("Eliminando archivo...");
                        break;
                    case "mkdirs":
                        outControl.println("Creando directorio...");
                        break;
                    case "rmdirs":
                        outControl.println("Eliminando directorio...");
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

    private static void listarArchivos(File directorio, PrintWriter outDatos) {
        if (directorio.exists() && directorio.isDirectory()) {
            File[] elementos = directorio.listFiles();
            if (elementos != null) {
                for (File elemento : elementos) {
                    if (elemento.isDirectory()) {
                        outDatos.println("Directorio: " + elemento.getName());
                    } else {
                        outDatos.println("Archivo: " + elemento.getName());
                    }
                }
            }else{
                outDatos.println("No se encontraron archivos");
            }
        } else {
            System.err.println("El directorio no existe o no es válido.");
        }
    }

    private static void enviarArchivo(String nombreArchivo, PrintWriter outDatos) {
        //enviar un archivo
        System.out.println("Nombre del archivo:" + nombreArchivo);
        System.out.println("Enviando archivo... ");
    }

    private static void recibirArchivo(String nombreArchivo, BufferedReader inDatos) {
        //recibir un archivo
        System.out.println("Recibiendo archivo... ");
    }


}

