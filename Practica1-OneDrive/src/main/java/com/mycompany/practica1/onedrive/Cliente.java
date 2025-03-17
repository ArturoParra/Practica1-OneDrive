package com.mycompany.practica1.onedrive;

import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        try {
            String dir = "127.0.0.1";
            Socket socketControl = new Socket(dir, 1234);
            Socket socketDatos = new Socket(dir, 1235);

            BufferedReader inControl = new BufferedReader(new InputStreamReader(socketControl.getInputStream()));
            PrintWriter outControl = new PrintWriter(socketControl.getOutputStream(), true);
            BufferedReader inDatos = new BufferedReader(new InputStreamReader(socketDatos.getInputStream()));
            PrintWriter outDatos = new PrintWriter(socketDatos.getOutputStream(), true);

            outControl.println("LIST");
            String respuesta;
            while ((respuesta = inDatos.readLine()) != null) {
                System.out.println(respuesta);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
