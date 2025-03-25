import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try {
            String dir = "127.0.0.1";
            Socket socketControl = new Socket(dir, 1234);
            Socket socketDatos = new Socket(dir, 1235);

            String rutaBase = "./dataclient";// Dirección que apunta al directorio de archivos del cliente

            File directorio = new File(rutaBase);// Ruta de directorio del cliente

            DataInputStream inDatos = new DataInputStream(socketDatos.getInputStream());
            DataOutputStream outDatos = new DataOutputStream(socketDatos.getOutputStream());

            DataInputStream inControl = new DataInputStream(socketControl.getInputStream());
            DataOutputStream outControl = new DataOutputStream(socketControl.getOutputStream());

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

                if(comandos.length > 2){
                    System.out.println("Demasiados argumentos, intente de nuevo.");
                    continue;
                }

                if(!comandos[0].equals("exit")){ //Verificar si el comando es exit
                    switch(comandos[0]){ //Verificar si el comando es del cliente o del servidor
                        // Comandos del cliente
                        case "help":
                            imprimirComandos();
                            break;
                        case "lsc":
                            listarArchivos(directorio);
                            break;
                        case "pwdc":
                            mostrarDirectorioActual(directorio);
                            break;
                        case "cdc":
                            if (comandos.length > 1){ // Verifica si el comando recibió el resto de argumentos
                                directorio = cambiarDirectorio(directorio, comandos[1]); // Actualizar el directorio
                            }else{
                                System.out.println("Error, usa el comando así: cdc <directorio> o cdc ..");
                            }
                            break;
                        case "upld":
                            enviarArchivo(outControl, inControl, outDatos, comandos, directorio);
                            break;
                        case "dwld":
                            recibirArchivo(outControl, inControl, inDatos, comandos, directorio);
                            break;
                        default: //Comandos del servidor
                            outControl.writeUTF(comando);
                            String respuesta2;
                            while ((respuesta2 = inControl.readUTF()) != null) {
                                if (respuesta2.equals("END")) { //Verifica si ya son todos los datos para que el socket no se bloquee
                                    break;
                                }
                                System.out.println(respuesta2);
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
        System.out.println("lsc: Listar archivos del cliente"); // listo (cliente)
        System.out.println("lss: Listar archivos del servidor"); // listo (servidor)
        System.out.println("pwdc: Mostrar directorio actual del cliente"); // listo (cliente)
        System.out.println("pwds: Mostrar directorio actual del servidor"); // (servidor)
        System.out.println("cdc: Cambiar directorio en el cliente"); // listo (cliente)
        System.out.println("cds: Cambiar directorio en el servidor"); // (servidor)
        System.out.println("dwld: Descargar archivo del servidor");// (servidor)
        System.out.println("upld: Subir archivo al servidor"); // listo (cliente)
        System.out.println("mkfiles: Crear archivo en el servidor"); // (servidor)
        System.out.println("mkfilec: Crear archivo en el cliente"); // (cliente)
        System.out.println("rmfiles: Eliminar archivo en el servidor"); // (servidor)
        System.out.println("rmfilec: Eliminar archivo en el cliente"); // (cliente)
        System.out.println("mkdirs: Crear directorio en el servidor"); // (servidor)
        System.out.println("mkdirc: Crear directorio en el cliente"); // (cliente)
        System.out.println("rmdirs: Eliminar directorio en el servidor"); // (servidor)
        System.out.println("rmdirc: Eliminar directorio en el cliente"); // (cliente)
        System.out.println("exit: Salir de la aplicación"); //listo
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

    private static void mostrarDirectorioActual(File Directorio){
        System.out.println("El directorio actual es: " + Directorio.getAbsolutePath());
    }

    private static File cambiarDirectorio(File Directorio, String nuevoDirectorio){
        File nuevoDir; // Crear la ruta del nuevo directorio

        if (nuevoDirectorio.equals("..")) {
            // Moverse al directorio padre
            nuevoDir = Directorio.getParentFile();

            if (nuevoDir != null && nuevoDir.exists()) {
                System.out.println("Moviéndose al directorio padre: " + nuevoDir.getAbsolutePath());
                return nuevoDir;
            } else {
                System.out.println("Ya estás en el directorio raíz, no puedes subir más.");
                return Directorio;
            }
        } else {
            // Moverse a un subdirectorio
            nuevoDir = new File(Directorio, nuevoDirectorio);

            if (nuevoDir.exists() && nuevoDir.isDirectory()) {
                System.out.println("Cambiando al directorio: " + nuevoDir.getAbsolutePath());
                return nuevoDir;
            } else {
                System.out.println("Error: El directorio no existe.");
                return Directorio;
            }
        }
    }

    private static void enviarArchivo(DataOutputStream outControl, DataInputStream inControl,  DataOutputStream outDatos,String[] comandos, File ruta) {
        try {
            File archivo = new File(ruta, comandos[1]);
            if (archivo.exists() && archivo.isFile()) {
                outControl.writeUTF(comandos[0]);
                outControl.writeUTF(comandos[1]);
                outControl.writeLong(archivo.length());
                outControl.writeUTF("END");
                System.out.println("Enviando archivo: " + archivo.getAbsolutePath());
                FileInputStream fis = new FileInputStream(archivo);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] buffer = new byte[1024];
                int leidos;
                while ((leidos = bis.read(buffer)) != -1) {
                    outDatos.write(buffer, 0, leidos);
                }
                bis.close();
                fis.close();
                System.out.println("Archivo enviado.");
            } else {
                System.out.println("El archivo no existe o no es válido.");
            }
        } catch (IOException e) {
            System.err.println("Error al enviar el archivo: " + e.getMessage());
        }
    }

    private static void recibirArchivo(DataOutputStream outControl, DataInputStream inControl, DataInputStream inDatos,String[] comandos, File ruta) {
        try{
            outControl.writeUTF(comandos[0]);
            outControl.writeUTF(comandos[1]);

            File file = new File(ruta, comandos[1]);

            long tamanoArchivo = inControl.readLong();
            System.out.println("Tamaño del archivo: " + tamanoArchivo);


            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[4096];
            int bytesLeidos;
            long bytesRestantes = tamanoArchivo;

            while (bytesRestantes > 0 && (bytesLeidos = inDatos.read(buffer, 0, (int)Math.min(buffer.length, bytesRestantes))) != -1) {
                bos.write(buffer, 0, bytesLeidos);
                bytesRestantes -= bytesLeidos;
            }

            bos.close();
            fos.close();

            System.out.println("Archivo recibido exitosamente.");


        }catch(IOException e){

        }
    }

}
