import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;


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
                                System.out.println("Sintáxis esperada: cdc <directorio> o cdc ..");
                            }
                            break;
                        case "upld":
                            enviarArchivo(outControl, inControl, outDatos, comandos, directorio);
                            break;
                        case "dwld":
                            recibirArchivo(outControl, inControl, inDatos, comandos, directorio);
                            break;
                        case "rnmec":
                            if (comandos.length > 1){
                                renombrarArchivo(comandos[1], directorio, in);
                            }else{
                                System.out.println("Sintáxis esperada: rnmec <directorio o archivo>");
                            }
                            break;
                        case "rnmes":
                            if (comandos.length > 1){
                                outControl.writeUTF(comandos[0]);
                                outControl.writeUTF(comandos[1]);
                                System.out.println("Ingrese el nombre nuevo: ");
                                String nombre = in.nextLine();
                                outControl.writeUTF(nombre);
                                System.out.println(inControl.readUTF());
                            }else{
                                System.out.println("Sintáxis esperada: rnmes <directorio o archivo>");
                            }
                            break;
                        case "rmc":
                            if (comandos.length > 1){
                                borrar(comandos[1], directorio);
                            }else{
                                System.out.println("Sintáxis esperada: rmdirc <directorio>");
                            }
                            break;
                        case "rms":
                            if (comandos.length > 1){
                                outControl.writeUTF(comandos[0]);
                                outControl.writeUTF(comandos[1]);
                                System.out.println(inControl.readUTF());
                            }else {
                                System.out.println("Sintáxis esperada: rms <directorio o archivo>");
                            }
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
        System.out.println("pwds: Mostrar directorio actual del servidor"); // (servidor) todo Patiño
        System.out.println("cdc: Cambiar directorio en el cliente"); // listo (cliente)
        System.out.println("cds: Cambiar directorio en el servidor"); // (servidor) todo Patiño
        System.out.println("dwld: Descargar archivo del servidor");// listo (servidor)
        System.out.println("upld: Subir archivo al servidor"); // listo (cliente)
        System.out.println("mkfiles: Crear archivo en el servidor"); // (servidor) todo Patiño
        System.out.println("mkfilec: Crear archivo en el cliente"); // (cliente) todo Patiño
        System.out.println("mkdirs: Crear directorio en el servidor"); // (servidor) todo Patiño
        System.out.println("mkdirc: Crear directorio en el cliente"); // (cliente) todo Patiño
        System.out.println("rms: Eliminar archivo o directorio en el servidor"); // listo (servidor)
        System.out.println("rmc: Eliminar archivo o directorio en el cliente"); // listo (cliente)
        System.out.println("rnmes: Renombrar archivo o directorio en el servidor"); // listo (servidor)
        System.out.println("rnmec: Renombrar archivo o directorio en el cliente"); // listo (cliente)
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
            } else if (archivo.exists() && archivo.isDirectory()) {
                outControl.writeUTF(comandos[0]);
                outControl.writeUTF(comandos[1]+".zip");
                File zipFile = comprimirCarpeta(archivo);
                outControl.writeLong(zipFile.length());
                outControl.writeUTF("END");

                System.out.println("Enviando ZIP: " + zipFile.getAbsolutePath());

                FileInputStream fis = new FileInputStream(zipFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                byte[] buffer = new byte[1024];
                int leidos;
                while ((leidos = bis.read(buffer)) != -1) {
                    outDatos.write(buffer, 0, leidos);
                }

                bis.close();
                fis.close();

                System.out.println("Archivo ZIP enviado.");
                zipFile.delete();

            } else {
                System.out.println("El archivo o directorio no existe o no es válido.");
            }
        } catch (IOException e) {
            System.err.println("Error al enviar el archivo: " + e.getMessage());
        }
    }

    private static void recibirArchivo(DataOutputStream outControl, DataInputStream inControl, DataInputStream inDatos,String[] comandos, File ruta) {
        try{
            outControl.writeUTF(comandos[0]);
            outControl.writeUTF(comandos[1]);

            File file = new File(ruta, comandos[1] + ".zip");

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

    private static File comprimirCarpeta(File carpeta) throws IOException {
        File zipFile = new File(carpeta.getParent(), carpeta.getName() + ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            agregarArchivosAlZip(carpeta, carpeta.getName(), zos);
        }
        return zipFile;
    }

    private static void agregarArchivosAlZip(File archivo, String nombreBase, ZipOutputStream zos) throws IOException {
        if (archivo.isDirectory()) {
            File[] archivos = archivo.listFiles();
            if (archivos != null) {
                for (File file : archivos) {
                    agregarArchivosAlZip(file, nombreBase + "/" + file.getName(), zos);
                }
            }
        } else {
            try (FileInputStream fis = new FileInputStream(archivo);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                ZipEntry zipEntry = new ZipEntry(nombreBase);
                zos.putNextEntry(zipEntry);
                byte[] buffer = new byte[1024];
                int bytesLeidos;
                while ((bytesLeidos = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesLeidos);
                }
                zos.closeEntry();
            }
        }
    }

    private static void renombrarArchivo(String nombreArchivo, File directorio, Scanner in){

        File archivo = new File(directorio, nombreArchivo);

        if (archivo.exists()) {
            System.out.println("Ingrese el nombre nuevo: ");
            String nuevoNombre = in.nextLine();
            File newFile = new File(directorio, nuevoNombre);
            if (archivo.renameTo(newFile)) {
                System.out.println("Renombrado exitosamente");
            }else{
                System.out.println("Error al renombrar");
            }
        }else{
            System.out.println("El archivo/directorio no existe en la ubicación actual");
        }
    }

    private static void borrar(String nombre, File directorio){
        File fichero = new File(directorio, nombre);
        if (fichero.exists()) {
            if(fichero.isDirectory()){
                if (eliminarRecursivo(fichero)) {
                    System.out.println("Eliminado exitosamente");
                } else {
                    System.out.println("Error al eliminar");
                }
            }else if(fichero.isFile()){
                if (fichero.delete()) {
                    System.out.println("Archivo eliminado exitosamente");
                }else{
                    System.out.println("Error al eliminar el archivo");
                }
            }else {
                System.out.println("El archivo/directorio no es válido");
            }

        }else{
            System.out.println("El directorio no existe en la ubicación actual");
        }
    }

    private static boolean eliminarRecursivo(File archivo) {
        if (archivo.isDirectory()) {
            for (File subArchivo : archivo.listFiles()) {
                eliminarRecursivo(subArchivo); // Llamada recursiva para cada archivo/subdirectorio
            }
        }
        return archivo.delete(); // Una vez vacío, elimina el directorio
    }

}
