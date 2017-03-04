package io.github.lethiandev.bluetoothclient;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.UUID;

/**
 * Wątek ustanawiający połączenie bluetooth między urządzeniem a serwerem.
 * Wysyła otwarty socket między wątkami poprzez handler.
 */
public class ConnectThread implements Runnable {
    /**
     * Stałe do identyfikowania wiadomości handlera.
     */
    public static final int MESSAGE_TIMEOUT = 0;
    public static final int MESSAGE_SOCKET = 1;

    /**
     * Unikalny identyfikator UUID.
     * Musi być taki sam jak na serwerze.
     */
    private static final String SERVER_UUID = "04c6093b-0000-1000-8000-00805f9b34fb";

    /**
     * Handle przekazujący wiadomości między wątkami.
     * Posłuży do przesłania otwartego socketa z serwerem.
     */
    private final Handler threadHandler;

    /**
     * Urządzenie bluetooth (serwer).
     */
    private final BluetoothDevice btDevice;

    /**
     * Socket bluetooth od wysyłania danych między urządzeniami przez bluetooth.
     */
    private final BluetoothSocket btSocket;

    /**
     * Konstruktor przygotowujący wątek do połączenia między urządzeniami.
     * @param handler handler wiadomości między wątkami
     * @param device urządzenie bluetooth (serwer)
     */
    public ConnectThread(Handler handler, BluetoothDevice device) {
        BluetoothSocket tmpSocket = null;

        threadHandler = handler;
        btDevice = device;

        try {
            UUID uuid = UUID.fromString(SERVER_UUID);
            // tworzymy tymczasowy socket z docelowym urządzeniem
            tmpSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
        }
        catch(Exception e) {
            // ups, coś poszło nie tak
            Log.e("ConnectThread", e.getMessage());
        }

        // jeżeli tymczasowy socket się połączy, zostaje ustawiony parametr klasy
        btSocket = tmpSocket;
    }

    /**
     * Metoda właściwa wątku.
     * Ustanawia połączenie między urządzeniami.
     */
    @Override
    public void run() {
        // przygotowanie wiadomości do wysłania (między wątkami)
        Message msg = new Message();
        msg.setTarget(threadHandler);

        try {
            Log.d("ConnectThread", "Oczekiwanie na urządzenie bluetooth...");

            // próba połączenia
            btSocket.connect();

            // ustanowiono połączenie, wysyłamy socket do głównego wątku
            msg.arg1 = MESSAGE_SOCKET;
            msg.obj = btSocket;
            msg.sendToTarget();
        }
        catch(Exception e) {
            // upłynął czas oczekiwania, bądź wystąpił inny problem
            Log.d("ConnectThread", e.getMessage());

            // wysyłamy wiadomość do głównego wątku o timeout-cie
            msg.arg1 = MESSAGE_TIMEOUT;
            msg.sendToTarget();

            // zamknij socket
            try {
                btSocket.close();
            } catch (Exception e2) {
                Log.e("ConnectThread", e2.getMessage());
            }
            return;
        }
    }
}
