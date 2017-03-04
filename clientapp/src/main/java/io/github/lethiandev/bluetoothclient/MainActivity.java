package io.github.lethiandev.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.io.IOException;
import java.util.Set;
import java.util.Vector;

/**
 * Aplikacja android.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Adapter i socket bluetooth.
     * Socket pobierany zostaje z wątku ConnectThread po ustanowieniu połączenia.
     */
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    /**
     * Panele pomocnicze.
     */
    private View connectPanel, buttonsPanel;

    /**
     * Lista wszystkich sparowanych urządzeń.
     */
    private Set<BluetoothDevice> pairedDevices = null;

    /**
     * Widget wyboru urządzenia do połączenia.
     */
    private Spinner pairedDevicesList = null;

    /**
     * Wątek ustanawiający połączenie oraz handler umożliwiający komunikację między wątkami.
     * Handler wysyła referencję do obiektu btSocket, gdy zostanie połączone urządzenie bt.
     */
    private Thread connectThread = null;
    private Handler threadHandler = null;

    /**
     * Tworzenie widoku aplikacji android.
     * Pobieramy adapter bluetooth, panele oraz widget od sparowanych urządzeń.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        connectPanel = findViewById(R.id.connectPanel);
        buttonsPanel = findViewById(R.id.buttonsPanel);

        pairedDevicesList = (Spinner)findViewById(R.id.pairedDevicesList);
    }

    /**
     * Uruchomienie aplikacji wczytuje sparowane urządzenia.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // lista sparowanych udządzeń - jako tablica tekstu
        Vector devicesList = new Vector();

        // pobiera listę sparowanych urządzeń
        pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("Bluetooth Nazwa Urz.", device.getName());
                Log.d("Bluetooth Adres MAC", device.getAddress());
                devicesList.addElement(device.getName() + " : " + device.getAddress());
            }
        }

        // ustawiamy widgetowi listę sparowanych urządzeń
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, devicesList
        );
        pairedDevicesList.setAdapter(arrayAdapter);
    }

    /**
     * Akcja gdy zostanie wciśnięty przycisk "Połącz".
     * @param v obiekt połączony z tym zdarzeniem (connect:Button"Połącz")
     */
    public void onConnectClick(View v) {
        ((Button) v).setText("Trwa łączenie...");
        setViewEnabled(connectPanel, false);

        makeConnection((Button) v);
    }

    /**
     * Akcja gdy zostanie wciśnięty przycisk "Poprzedni".
     * @param v obiekt połączony z tym zdarzeniem (Button"Poprzedni")
     */
    public void onPreviousClick(View v) {
        try {
            Log.d("MainActivity", "Wykonywanie onPreviousClick (1)");

            // wysyłamy wiadomość do serwera przez strumień danych bluetooth
            btSocket.getOutputStream().write(1);
        } catch (IOException e) {
            // ups, coś poszło nie tak
            Log.e("MainActivity", e.getMessage());
        }
    }

    /**
     * Akcja gdy zostanie wciśnięty przycisk "Następny".
     * @param v obiekt połączony z tym zdarzeniem (Button"Następny")
     */
    public void onNextClick(View v) {
        try {
            Log.d("MainActivity", "Wykonywanie onNextClick (2)");

            // wysyłamy wiadomość do serwera przez strumień danych bluetooth
            btSocket.getOutputStream().write(2);
        } catch (IOException e) {
            // ups, coś poszło nie tak
            Log.e("MainActivity", e.getMessage());
        }
    }

    /**
     * Tworzy wątek ustanawiający połączenie między serwerem a aplikacją android.
     * @param connect referencja na przycisk do przywrócenia tekstu podczas niepowodzenia
     */
    private void makeConnection(final Button connect) {
        // pobieramy aktualnie "wybrane" urządzenie bluetooth z listy
        BluetoothDevice device = getSelectedBluetoothDevice();

        // tworzymy handler, który posłuży do wymiany danych między wątkami
        threadHandler = new Handler() {
            public void handleMessage(Message msg) {
                // połączenie powiodło się
                if (msg.arg1 == ConnectThread.MESSAGE_SOCKET) {
                    btSocket = (BluetoothSocket)msg.obj;
                    setCurrentPanel(buttonsPanel);
                }
                // upłynął czas oczekiwania (bądź wystąpił problem), odblokuj panel
                else if (msg.arg1 == ConnectThread.MESSAGE_TIMEOUT) {
                    setViewEnabled(connectPanel, true);
                    connect.setText("Połącz");
                }
            }
        };

        // tworzymy i uruchamiamy wątek połączenia
        connectThread = new Thread(new ConnectThread(threadHandler, device));
        connectThread.start();
    }

    /**
     * Funkcja pomocnicza wyłączająca rekursywnie dzieci danego kontenera.
     * Elementy wewnątrz kontenera stają się wyszarzone.
     * @param view główny kontener (np. panel)
     * @param enabled czy elementy mają być włączone (bądź wyłączone)
     */
    private void setViewEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup)view;

            for (int idx = 0; idx < viewGroup.getChildCount(); idx++) {
                setViewEnabled(viewGroup.getChildAt(idx), enabled);
            }
        }
    }

    /**
     * Funkcja pomocnicza ustawiająca widok na dany panel, ukrywając pozostałę panele.
     * @param panel aktualny panel do wyświetlenia
     */
    private void setCurrentPanel(View panel) {
        connectPanel.setVisibility(View.INVISIBLE);
        buttonsPanel.setVisibility(View.INVISIBLE);
        panel.setVisibility(View.VISIBLE);
    }

    /**
     * Funkcja pomocnicza zwracająca aktualnie wybrane urządzenie bluetooth z listy.
     * @return urządzenie bluetooth jako BluetoothDevice (zamiast nazwy urządzenia)
     */
    private BluetoothDevice getSelectedBluetoothDevice() {
        int pairedDeviceIndex = pairedDevicesList.getSelectedItemPosition();
        return (BluetoothDevice) pairedDevices.toArray()[pairedDeviceIndex];
    }
}
