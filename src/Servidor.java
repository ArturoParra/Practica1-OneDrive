
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

public class Servidor {
    public static void main(String[] args) {
        try {
            ServerSocket sControl = new ServerSocket(1234);
            sControl.setReuseAddress(true);
            ServerSocket sDatos = new ServerSocket(1235);
            sDatos.setReuseAddress(true);
            System.out.println("Servidor de control iniciado en el puerto: " + sControl.getLocalPort());
            System.out.println("Servidor de datos iniciado en el puerto: " + sDatos.getLocalPort());

            String rutaBase = "./dataserver";// Dirección que apunta al directorio de archivos del servidor

            File directorio = new File(rutaBase);

            for(;;){
                // Aceptar conexión del cliente en el socket de control
                Socket socketControl = sControl.accept();
                System.out.println("Cliente conectado: " + socketControl.getInetAddress());

                // Aceptar conexión del cliente en el socket de datos
                Socket socketDatos = sDatos.accept();
                System.out.println("Conexión de datos establecida");

                // Procesar comandos del cliente
                procesarComandos(socketControl, socketDatos, directorio);
            }
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
    }

    private static void procesarComandos(Socket socketControl, Socket socketDatos, File directorio) {
        try{

            DataInputStream inDatos = new DataInputStream(socketDatos.getInputStream());
            DataOutputStream outDatos = new DataOutputStream(socketDatos.getOutputStream());

            DataInputStream inControl = new DataInputStream(socketControl.getInputStream());
            DataOutputStream outControl = new DataOutputStream(socketControl.getOutputStream());

            String comando;
            while ((comando = inControl.readUTF()) != null) {
                System.out.println("Comando recibido: " + comando);
                // Procesar el comando
                switch (comando) {
                    case "lss":
                        listarArchivos(directorio, outControl);
                        break;
                    case "dwld":
                        enviarArchivo(outControl, inControl, outDatos, directorio);
                        break;
                    case "upld":
                        recibirArchivo(inControl, inDatos, directorio);
                        break;
                    case "mkfiles":
                        outControl.writeUTF("Creando archivo...");
                        break;
                    case "mkdirs":
                        outControl.writeUTF("Creando directorio...");
                        break;
                    case "rms":
                        borrar(inControl, outControl, directorio);
                        break;
                    case "rnmes":
                        renombrarArchivo(inControl, outControl, directorio);
                        break;
                    default:
                        outControl.writeUTF("Comando no reconocido");
                        outControl.writeUTF("END");
                        break;
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }


    private static void listarArchivos(File directorio, DataOutputStream outControl) {
        try {
            if (directorio.exists() && directorio.isDirectory()) {
                File[] elementos = directorio.listFiles();
                if (elementos != null) {
                    for (File elemento : elementos) {
                        if (elemento.isDirectory()) {
                            outControl.writeUTF("Directorio: " + elemento.getName());
                        } else {
                            outControl.writeUTF("Archivo: " + elemento.getName());
                        }
                    }
                }else{
                    outControl.writeUTF("No se encontraron archivos");
                }
            } else {
                System.err.println("El directorio no existe o no es válido.");
            }
            outControl.writeUTF("END");// Marca de fin de la comunicación
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void enviarArchivo(DataOutputStream outControl, DataInputStream inControl, DataOutputStream outDatos, File directorio) {

        try{
            String nombreArchivo = inControl.readUTF();
            System.out.println("Nombre de archivo:" + nombreArchivo);

            File archivo = new File(directorio, nombreArchivo);

            if (archivo.exists() && archivo.isFile()) {
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
                File zipFile = comprimirCarpeta(archivo);
                outControl.writeLong(zipFile.length());
                outControl.writeUTF("END");

                System.out.println("Enviando ZIP: " + zipFile.getAbsolutePath());

                FileInputStream fis = new FileInputStream(zipFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                byte[] buffer = new byte[1024];
                int leidos;
                while((leidos = bis.read(buffer)) != -1) {
                    outDatos.write(buffer, 0, leidos);
                }

                bis.close();
                fis.close();

                System.out.println("Archivo ZIP enviado");
                zipFile.delete();

            } else {
                System.out.println("El archivo no existe o no es válido.");
            }



        } catch (IOException e) {
            //System.out.println("Error al enviar el archivo: " + e.getMessage());
        }

    }

    private static void recibirArchivo(DataInputStream inControl, DataInputStream inDatos, File directorio) {
        try{
            String nombreArchivo = inControl.readUTF();
            System.out.println("Recibiendo archivo: " + nombreArchivo);

            File file = new File(directorio, nombreArchivo);

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

    private static void renombrarArchivo( DataInputStream inControl, DataOutputStream outControl, File directorio) {

        try {
            String nombreArchivo = inControl.readUTF();
            String nombreNuevo = inControl.readUTF();
            File archivo = new File(directorio, nombreArchivo);


            System.out.println("Archivo a renombrar: " + archivo.getAbsolutePath());
            System.out.println("Nuevo nombre: " + nombreNuevo);

            if (archivo.exists()) {
                System.out.println("Ingrese el nombre nuevo: ");
                File newFile = new File(directorio, nombreNuevo);
                if (archivo.renameTo(newFile)) {
                    outControl.writeUTF("Renombrado exitosamente");
                }else{
                    outControl.writeUTF("Error al renombrar");
                }
            }else{
                outControl.writeUTF("El archivo/directorio no existe en la ubicación actual");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void borrar(DataInputStream inControl, DataOutputStream outControl, File directorio){
        try {
            String nombre = inControl.readUTF();
            File fichero = new File(directorio, nombre);
            if (fichero.exists()) {
                if(fichero.isDirectory()){
                    if (eliminarRecursivo(fichero)) {
                        outControl.writeUTF("Eliminado exitosamente");
                    } else {
                        outControl.writeUTF("Error al eliminar");
                    }
                }else if(fichero.isFile()){
                    if (fichero.delete()) {
                        outControl.writeUTF("Archivo eliminado exitosamente");
                    }else{
                        outControl.writeUTF("Error al eliminar el archivo");
                    }
                }else {
                    outControl.writeUTF("El archivo/directorio no es válido");
                }

            }else{
                outControl.writeUTF("El directorio no existe en la ubicación actual");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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

