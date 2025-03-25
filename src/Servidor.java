
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.*;
import java.net.Socket;
import javax.swing.JFileChooser;

public class Servidor {
    public static void main(String[] args) {
        try {
            ServerSocket sControl = new ServerSocket(1234);
            sControl.setReuseAddress(true);
            ServerSocket sDatos = new ServerSocket(1235);
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

            DataInputStream inDatos = new DataInputStream(socketDatos.getInputStream());
            DataOutputStream outDatos = new DataOutputStream(socketDatos.getOutputStream());

            DataInputStream inControl = new DataInputStream(socketControl.getInputStream());
            DataOutputStream outControl = new DataOutputStream(socketControl.getOutputStream());

            String rutaBase = "./dataserver";// Dirección que apunta al directorio de archivos del servidor

            String comando;
            while ((comando = inControl.readUTF()) != null) {
                System.out.println("Comando recibido: " + comando);
                // Procesar el comando
                switch (comando) {
                    case "lss":
                        File directorio = new File(rutaBase);
                        listarArchivos(directorio, outControl);
                        break;
                    case "dwld":
                        enviarArchivo(outControl, inControl, outDatos, rutaBase);
                        break;
                    case "upld":
                        recibirArchivo(inControl, inDatos, rutaBase);
                        break;
                    case "mkfiles":
                        outControl.writeUTF("Creando archivo...");
                        break;
                    case "rmfiles":
                        outControl.writeUTF("Eliminando archivo...");
                        break;
                    case "mkdirs":
                        outControl.writeUTF("Creando directorio...");
                        break;
                    case "rmdirs":
                        outControl.writeUTF("Eliminando directorio...");
                        break;
                    default:
                        outControl.writeUTF("Comando no reconocido");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private static void enviarArchivo(DataOutputStream outControl, DataInputStream inControl, DataOutputStream outDatos, String rutaBase) {
        try{
            String nombreArchivo = inControl.readUTF();
            System.out.println("Nombre de archivo:" + nombreArchivo);

            File archivo = new File(rutaBase, nombreArchivo);

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
            } else {
                System.out.println("El archivo no existe o no es válido.");
            }
        } catch (IOException e) {
            //System.out.println("Error al enviar el archivo: " + e.getMessage());
        }

    }

    private static void recibirArchivo(DataInputStream inControl, DataInputStream inDatos, String rutaBase) {
        try{
            String nombreArchivo = inControl.readUTF();
            System.out.println("Recibiendo archivo: " + nombreArchivo);

            File file = new File(rutaBase, nombreArchivo);

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

