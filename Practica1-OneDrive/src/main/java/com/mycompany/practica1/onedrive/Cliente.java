package com.mycompany.practica1.onedrive;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JFileChooser;

public class Cliente {
    public static void main(String[] args) {
        try {
            String dir = "127.0.0.1";
            Socket socketControl = new Socket(dir, 1234);
            Socket socketDatos = new Socket(dir, 1235);

            File directorio = new File("./dataclient");// Ruta de directorio del cliente

            BufferedReader inControl = new BufferedReader(new InputStreamReader(socketControl.getInputStream()));
            PrintWriter outControl = new PrintWriter(socketControl.getOutputStream(), true);
            BufferedReader inDatos = new BufferedReader(new InputStreamReader(socketDatos.getInputStream()));
            PrintWriter outDatos = new PrintWriter(socketDatos.getOutputStream(), true);
            if (socketControl.isConnected() && !socketControl.isClosed()) {
                System.out.println("Conexión exitosa.");
            } else {
                System.out.println("Conexión fallida.");
            }

            Scanner in = new Scanner(System.in);

            String comando;
            System.out.println("Bienvenido, Use el comando help para ver los comandos disponibles y el comando exit para salir de la aplicación.");
            while(true){
                System.out.print(">> ");
                comando = in.nextLine();

                String[] comandos = comando.split(" ");

                if(!comandos[0].equals("exit")){ //Verificar si el comando es exit
                    switch(comandos[0]){ //Verificar si el comando es del cliente o del servidor
                        // Comandos del cliente
                        case "help":
                            imprimirComandos();
                            break;
                        case "lsc":
                            listarArchivos(directorio);
                            break;
                        case "upld":
                            File archivo = new File(comandos[1]);
                            if(archivo.exists()){
                                
                            }
                            enviarArchivo(archivo, outDatos);
                            break;
                        default: //Comandos del servidor
                            outControl.println(comando);
                            String respuesta = inControl.readLine();
                            if(respuesta != null){ //Respuesta del socket de control
                                System.out.println(respuesta);
                            }
                            if (comando.equals("lss") || comando.equals("dwld") || comando.equals("upld")) { //Respuesta del socket de datos
                                String respuesta2;
                                while ((respuesta2 = inDatos.readLine()) != null) {
                                    if (respuesta2.equals("END")) { //Verifica si ya son todos los datos para que el socket no se bloquee
                                        break;
                                    }
                                    System.out.println(respuesta2);
                                }
                            }
                            break;
                    }
                }else{ // Caso en el que el comando sea exit
                    System.out.println("Saliendo de la aplicación..., Hasta luego.");
                    break;
                }

            }

            socketControl.close();
            socketDatos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //Función que imprime el menú de comandos
    private static void imprimirComandos(){
        System.out.println("Comandos disponibles:");
        System.out.println("lsc: Listar archivos del cliente");
        System.out.println("lss: Listar archivos del servidor");
        System.out.println("pwdc: Mostrar directorio actual del cliente");
        System.out.println("pwds: Mostrar directorio actual del servidor");
        System.out.println("cdc: Cambiar directorio en el cliente");
        System.out.println("cds: Cambiar directorio en el servidor");
        System.out.println("dwld: Descargar archivo del servidor");
        System.out.println("upld: Subir archivo al servidor");
        System.out.println("mkfiles: Crear archivo en el servidor");
        System.out.println("mkfilec: Crear archivo en el cliente");
        System.out.println("rmfiles: Eliminar archivo en el servidor");
        System.out.println("rmfilec: Eliminar archivo en el cliente");
        System.out.println("mkdirs: Crear directorio en el servidor");
        System.out.println("mkdirc: Crear directorio en el cliente");
        System.out.println("rmdirs: Eliminar directorio en el servidor");
        System.out.println("rmdirc: Eliminar directorio en el cliente");
        System.out.println("exit: Salir de la aplicación");
    }

    private static void listarArchivos(File directorio) {
        File[] elementos = directorio.listFiles();
        if (elementos != null) {
            for (File elemento : elementos) {
                if (elemento.isDirectory()) {
                    System.out.println("Directorio: " + elemento.getName());
                } else {
                    System.out.println("Archivo: " + elemento.getName());
                }
            }
        }else{
            System.out.println("El directorio está vacío.");
        }
    }

    private static void enviarArchivo(File archivo, PrintWriter outDatos) {
        System.out.println(archivo.getAbsolutePath());
    }

}
