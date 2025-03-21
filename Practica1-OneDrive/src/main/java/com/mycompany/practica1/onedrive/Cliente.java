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
            OutputStream outDatos2 = socketDatos.getOutputStream();
            DataInputStream inDatos2 = new DataInputStream(socketDatos.getInputStream());
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
                            System.out.println("Socket conectado: " + socketDatos.isConnected());
                            if(comandos.length > 1){ // Verifica si el comando recibió el resto de argumentos
                                File archivo = new File(directorio, comandos[1]); // Se crea la referencia al archivo dentro de la ruta de carpeta de archivos del cliente
                                if(archivo.exists() && archivo.isFile()){ // Verifica si existe el archivo y si es un archivo
                                    System.out.println("El archivo existe");
                                    enviarArchivo(archivo, outDatos2, outControl, comandos[0]);
                                }else{
                                    System.out.println("El archivo no existe en este directorio");
                                    break;
                                }
                                String respuesta = inControl.readLine();
                                if(respuesta != null){ //Respuesta del socket de control
                                    System.out.println(respuesta);
                                }
                                String respuesta2;
                                while ((respuesta2 = inDatos.readLine()) != null) {
                                    if (respuesta2.equals("END")) { //Verifica si ya son todos los datos para que el socket no se bloquee
                                        System.out.println("Se recibe el end");
                                        break;
                                    }
                                    System.out.println(respuesta2);
                                }
                            }else{
                                System.out.println("Comando incompleto: Ingrese el nombre del archivo a subir");
                            }
                            break;
                        default: //Comandos del servidor
                            outControl.println(comando);
                            String respuesta = inControl.readLine();
                            if(respuesta != null){ //Respuesta del socket de control
                                System.out.println(respuesta);
                            }
                            if (comando.equals("lss") || comando.equals("dwld")) { //Respuesta del socket de datos
                                String respuesta2;
                                while ((respuesta2 = inControl.readLine()) != null) {
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

    private static void enviarArchivo(File archivo, OutputStream outDatos2, PrintWriter outControl, String comando) {
        try {
            outControl.println(comando); // Notificar al servidor que se enviará un archivo
            outControl.println(archivo.getName());
            outControl.flush();

            // Enviar el tamaño del archivo al servidor
            DataOutputStream dos = new DataOutputStream(outDatos2);
            dos.writeLong(archivo.length());
            dos.flush();

            // Enviar los datos del archivo
            FileInputStream fis = new FileInputStream(archivo);
            byte[] buffer = new byte[4096]; // 4 KB
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesLeidos);
            }

            System.out.println("Archivo enviado con éxito.");


        } catch (IOException e) {
            //System.err.println("Error al enviar el archivo: " + e.getMessage());
        }
    }

}
