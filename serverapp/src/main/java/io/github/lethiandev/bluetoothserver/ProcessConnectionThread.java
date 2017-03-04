package io.github.lethiandev.bluetoothserver;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.InputStream;

import javax.microedition.io.StreamConnection;

/**
 * Wątek wykonujący rozkazy otrzymywane z połączonego urządzenia bluetooth (np. ze smartfona).
 * Podstawowe rozkazy to:
 * - CMD_EXIT - utracono połączenie z klientem
 * - CMD_KEY_LEFT - kursor w lewo
 * - CMD_KEY_RIGHT - kursor w prawo
 */
public class ProcessConnectionThread implements Runnable {
    private StreamConnection connection;

    /**
     * Stałe komunikatów.
     */
    private static final int CMD_EXIT = -1;
    private static final int CMD_KEY_LEFT = 1;
    private static final int CMD_KEY_RIGHT = 2;

    /**
     * Konstruktor ustawiający parametry od pobierania rozkazów.
     * @param connection ustawione połączenie z urządzeniem bluetooth.
     */
    public ProcessConnectionThread(StreamConnection connection) {
        this.connection = connection;
    }

    /**
     * Główna funkcja wątku.
     */
    @Override
    public void run() {
        InputStream inputStream;

        try {
            // otwieramy strumień danych wejściowych
            inputStream = connection.openInputStream();
            System.out.println("Oczekiwanie na wiadomość...");

            while (true) {
                // czytanie danych ze strumienia blokuje wątek (cecha klasy InputStream)
                int command = inputStream.read();

                // jeżeli klient zostanie rozłączony zamknij serwer
                if (command == CMD_EXIT) {
                    System.out.println("Koniec połączenia");
                    break;
                }

                // wykonaj komendę którą przesłał klient
                processCommand(command);
            }
        }
        catch (Exception e) {
            // ups, coś poszło nie tak
            e.printStackTrace();
        }
    }

    /**
     * Wykonuje rozkaz.
     * @param command identyfikator rozkazu.
     */
    private void processCommand(int command) {
        Robot robot;

        try {
            // robot wykonuje systemowe zdarzenia
            robot = new Robot();

            switch(command) {
                // wykonaj: strzałka w lewo
                case CMD_KEY_LEFT:
                    robot.keyPress(KeyEvent.VK_LEFT);
                    robot.keyRelease(KeyEvent.VK_LEFT); // po keyPress należy zwolnić przycisk (!!!)
                    System.out.println("Wykonano komendę CMD_KEY_LEFT");
                    break;
                // wykonaj: strzałka w prawo
                case CMD_KEY_RIGHT:
                    robot.keyPress(KeyEvent.VK_RIGHT);
                    robot.keyRelease(KeyEvent.VK_RIGHT); // po keyPress należy zwolnić przycisk (!!!)
                    System.out.println("Wykonano komendę CMD_KEY_RIGHT");
                    break;
            }
        }
        catch (Exception e) {
            // ups, coś poszło nie tak
            e.printStackTrace();
        }
    }
}
