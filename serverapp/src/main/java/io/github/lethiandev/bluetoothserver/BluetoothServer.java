package io.github.lethiandev.bluetoothserver;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

/**
 * Serwer aplikacji bluetooth (konsola).
 * Otwiera streaming oczekując połączenia z innym urządzeniem bluetooth.
 * Listener blokuje działanie aplikacji aż któreś sparowane urządzenie nie połączy się.
 */
public class BluetoothServer {
    /**
     * Unikalny identyfikator UUID.
     * Musi być taki sam jak w aplikacji android.
     */
    private static final String SERVER_UUID = "04c6093b-0000-1000-8000-00805f9b34fb";

    /**
     * Wejście programu konsolowego.
     * @param args
     */
    public static void main(String [] args) {
        System.out.println("Bluetooth Server");

        initConnection();
    }

    /**
     * Inicjuje połączenie tworząc listenera (notifiera).
     */
    public static void initConnection() {
        LocalDevice local = null;

        StreamConnectionNotifier notifier = null;

        try {
            local = LocalDevice.getLocalDevice();
            // metoda setDiscoverable ustawia widoczność bluetooth
            // zakomentowane bo wymaga uprawnień administratora
            //local.setDiscoverable(DiscoveryAgent.GIAC);

            // adres do nasłuchu - wymagany jest taki sam UUID dla serwera jak i klienta
            String url = "btspp://localhost:" + SERVER_UUID.replace("-", "") + ";name=RemoteBluetooth";

            // otwieramy połączenie
            notifier = (StreamConnectionNotifier) Connector.open(url);
        }
        catch (Exception e) {
            // ups, coś poszło nie tak
            e.printStackTrace();
            return;
        }

        // po utworzeniu listenera, oczekujemy na połączenie
        waitForConnection(notifier);
    }

    /**
     * Oczekuje na połączenie urządzenia bluetooth.
     * @param notifier listener utworzony przez Connector.open
     */
    public static void waitForConnection(StreamConnectionNotifier notifier) {
        StreamConnection connection;

        try {
            System.out.println("Oczekiwanie na połączenie...");

            // akceptujemy każde połączenie o takim samym UUID
            connection = notifier.acceptAndOpen();

            // uruchamiamy nowy proces który będzie wykonywać rozkazy wysyłane od klienta
            Thread processThread = new Thread(new ProcessConnectionThread(connection));
            processThread.start();
        } catch (Exception e) {
            // ups, coś poszło nie tak
            e.printStackTrace();
            return;
        }
    }
}
