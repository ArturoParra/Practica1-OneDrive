import java.net.*; //Manejo de sockets
import java.io.*; //  Entrada y salida de datos, manipulación de archivos
//Manipulación de archivos ZIP.
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

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
                    case "rms":
                        borrar(inControl, outControl, directorio);
                        break;
                    case "rnmes":
                        renombrarArchivo(inControl, outControl, directorio);
                        break;
                    case "pwds":
                        mostrarDirectorioActual(directorio, outControl);
                        break;
                    case "cds":
                        directorio = cambiarDirectorio(directorio, outControl, inControl);
                        break;
                    case "mkfiles":
                        crearArchivo(inControl, outControl, directorio);
                        break;
                    case "mkdirs":
                        crearDirectorio(inControl, outControl, directorio);
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
                } else {
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
        try {
            String nombreArchivo = inControl.readUTF();
            System.out.println("Nombre de archivo:" + nombreArchivo);

            File archivo = new File(directorio, nombreArchivo);

            if (archivo.exists() && archivo.isFile()) {
                outControl.writeUTF("FILE");
                outControl.writeLong(archivo.length());
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
                outControl.writeUTF("DIRECTORIO");
                File zipFile = comprimirCarpeta(archivo);
                outControl.writeLong(zipFile.length());

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
                outControl.writeUTF("El archivo no existe o no es válido.");
            }
        } catch (IOException e) {
            //System.out.println("Error al enviar el archivo: " + e.getMessage());
        }
    }

    private static File comprimirCarpeta(File carpeta) throws IOException {
        File zipFile = new File(carpeta.getParent(), carpeta.getName() + ".zip");
        try {
            ZipFile zip = new ZipFile(zipFile);
            zip.addFolder(carpeta, new ZipParameters());
            System.out.println("Carpeta comprimida en: " + zipFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error al comprimir la carpeta: " + e.getMessage());
            throw new IOException("No se pudo comprimir la carpeta.");
        }
        return zipFile;
    }

    private static void recibirArchivo(DataInputStream inControl, DataInputStream inDatos, File directorio) {
        try {
            String nombreArchivo = inControl.readUTF();
            System.out.println("Recibiendo archivo: " + nombreArchivo);

            File file = new File(directorio, nombreArchivo);
            long tamanoArchivo = inControl.readLong();
            System.out.println("Tamaño del archivo: " + tamanoArchivo + " bytes");

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[4096];
            int bytesLeidos;
            long bytesRestantes = tamanoArchivo;

            while (bytesRestantes > 0 && (bytesLeidos = inDatos.read(buffer, 0, (int) Math.min(buffer.length, bytesRestantes))) != -1) {
                bos.write(buffer, 0, bytesLeidos);
                bytesRestantes -= bytesLeidos;
            }

            bos.close();
            fos.close();

            // Si es un ZIP, descomprimirlo
            if (nombreArchivo.endsWith(".zip")) {
                descomprimirArchivo(file.getCanonicalPath(), directorio.getCanonicalPath());
                file.delete();
            }

        } catch (IOException e) {
            System.err.println(" Error al recibir el archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void descomprimirArchivo(String zipPath, String destinoPath) {
        try {
            File zipFile = new File(zipPath);
            File destino = new File(destinoPath);

            if (!zipFile.exists()) { // Verificar que el ZIP existe antes de extraer
                System.err.println("Error: El archivo ZIP no existe: " + zipFile.getAbsolutePath());
                return;
            }

            if (!destino.exists()) { // Crear la carpeta de destino si no existe
                boolean creado = destino.mkdirs();
                if (creado) {
                    System.out.println("Carpeta destino creada: " + destinoPath);
                } else {
                    System.err.println("Error: No se pudo crear la carpeta destino.");
                    return;
                }
            }

            ZipFile zip = new ZipFile(zipFile); // Descomprimir
            zip.extractAll(destinoPath);
            System.out.println("Archivo ZIP extraído correctamente en: " + destinoPath);

            File[] archivos = destino.listFiles(); // Verificar si la extracción tuvo éxito
            if (archivos != null && archivos.length > 0) {
                System.out.println("Archivos extraídos");
            } else {
                System.err.println("No se encontraron archivos extraídos.");
            }

        } catch (ZipException e) {
            System.err.println("Error al extraer el ZIP: " + e.getMessage());
            e.printStackTrace();
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
                } else if(fichero.isFile()) {
                    if (fichero.delete()) {
                        outControl.writeUTF("Archivo eliminado exitosamente");
                    } else {
                        outControl.writeUTF("Error al eliminar el archivo");
                    }
                } else {
                    outControl.writeUTF("El archivo/directorio no es válido");
                }

            } else {
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
                } else {
                    outControl.writeUTF("Error al renombrar");
                }
            } else {
                outControl.writeUTF("El archivo/directorio no existe en la ubicación actual");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mostrarDirectorioActual(File Directorio, DataOutputStream outControl) {
        try {
            outControl.writeUTF("El directorio actual del servidor es: " + Directorio.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File cambiarDirectorio(File Directorio, DataOutputStream outControl, DataInputStream inControl){
        try {
            File nuevoDir; // Crear la ruta del nuevo directorio

            String nuevoDirectorio = inControl.readUTF();

            if (nuevoDirectorio.equals("..")) {
                nuevoDir = Directorio.getParentFile(); // Moverse al directorio padre

                if (nuevoDir != null && nuevoDir.exists()) {
                    outControl.writeUTF("Moviéndose al directorio padre: " + nuevoDir.getAbsolutePath());
                    return nuevoDir;
                } else {
                    outControl.writeUTF("Ya estás en el directorio raíz, no puedes subir más.");
                    return Directorio;
                }
            } else {
                nuevoDir = new File(Directorio, nuevoDirectorio); // Moverse a un subdirectorio
                if (nuevoDir.exists() && nuevoDir.isDirectory()) {
                    outControl.writeUTF("Cambiando al directorio: " + nuevoDir.getAbsolutePath());
                    return nuevoDir;
                } else {
                    outControl.writeUTF("Error: El directorio no existe.");
                    return Directorio;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void crearArchivo(DataInputStream inControl, DataOutputStream outControl, File directorio){
        try {
            String nombreArchivo = inControl.readUTF();
            File archivo = new File(directorio, nombreArchivo);
            if (archivo.exists()) {
                outControl.writeUTF("El archivo ya existe.");
            } else {
                if (archivo.createNewFile()) {
                    outControl.writeUTF("Archivo creado exitosamente: " + archivo.getAbsolutePath());
                } else {
                    outControl.writeUTF("No se pudo crear el archivo.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error al crear el archivo: " + e.getMessage());
        }
    }

    private static void crearDirectorio(DataInputStream inControl, DataOutputStream outControl, File directorio){
        try {
            String nombreDirectorio = inControl.readUTF();
            File nuevoDirectorio = new File(directorio, nombreDirectorio);
            if(nuevoDirectorio.exists()){
                outControl.writeUTF("El directorio ya existe.");
            } else {
                if(nuevoDirectorio.mkdir()){
                    outControl.writeUTF("Directorio creado exitosamente: " + nuevoDirectorio.getAbsolutePath());
                } else {
                    outControl.writeUTF("No se pudo crear el directorio.");
                }
            }
        }catch (Exception e){
            System.err.println("Error al crear el directorio: " + e.getMessage());
        }
    }
}

