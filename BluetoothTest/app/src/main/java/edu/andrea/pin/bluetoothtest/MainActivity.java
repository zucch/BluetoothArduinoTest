/*
 * Copyright (C) 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.andrea.pin.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import edu.andrea.pin.bluetoothtest.Models.Coordinates;
import edu.andrea.pin.bluetoothtest.Utils.Constants;
import edu.andrea.pin.bluetoothtest.bluetooth.BluetoothService;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends IActivity {

    // private bluetooth variables
    private BluetoothService mChatService;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mConnectedDeviceName = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // trick
    private Activity _me;

    // UI variables
    private TextView tvStatus;
    private TextView tvResponse;
    private LinearLayout chartLyt;
    private LinearLayout llWaitBar;

    private List<Coordinates> _Coordinates;
    private String received;
    private int _Count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        if (mChatService != null) {
            mChatService.stop();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        _me = this;
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvResponse = (TextView) findViewById(R.id.tvResponse);
        chartLyt = (LinearLayout) findViewById(R.id.chart);
        llWaitBar = (LinearLayout) findViewById(R.id.llWaitTime);

        chartLyt.setVisibility(View.GONE);
        llWaitBar.setVisibility(View.GONE);

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

        if (application != null && application.ReceivedData != null && !application.ReceivedData.isEmpty()){
            if (mainHandler != null) {
                if (_Coordinates == null){
                    _Coordinates = new ArrayList<Coordinates>();
                }
                else{
                    _Coordinates.clear();
                }
                received = application.ReceivedData;
                mainHandler.postDelayed(uiRunnable, 100);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.secure_connect_scan) {
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        }
        else if (id == R.id.insecure_connect_scan) {
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        }
        else if (id ==  R.id.discoverable) {
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(Constants.BT_LOG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    //TODO: this.finish();
                }
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    // UI INTERACTIONS
    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(Constants.BT_LOG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void setStatus(String status, int color){
        if (tvStatus != null){
            tvStatus.setText(status);
            tvStatus.setTextColor(color);
        }
    }

    // CHART CREATION
    boolean creating = false;
    private void drawChart(){
        // non disegnare un nuovo grafico se ne stai già disegnando 1
        // non fare nulla se non hai informazioni da visualizzare
        if (creating || received == null || received.isEmpty())
            return;

        creating = true;

        received = received.substring(received.lastIndexOf('*')+1);
        received = received.replaceAll("\r\n", ";");
        received = received.replaceAll("\t", ";");
        received = received.replaceAll("\\r\\n", ";");
        received = received.replaceAll("\\t", ";");
        received = received.replaceAll(" ", "");
        received = received.replaceAll("\\.\\.", ".");
        received = received.replaceAll(";;", ";");
        received = received.replaceAll("O", "");

        while (received.startsWith(";")) {
            received = received.substring(1);
        }

        if (application.ReceivedData == null || !application.ReceivedData.equals(received)) {
            // nuove informazioni ricevute da arduino, salvo in una variabile globale
            application.ReceivedData = received;
        }

        String split[] = received.split(";");
        // converti la stringa ricevuta in coppie di coordinate
        float maxX = -10;
        for (int i = 0; i < split.length; i += 2) {
            try{
                Coordinates c = new Coordinates();
                c.x = Float.valueOf(split[i]);
                c.y = Float.valueOf(split[i+1]);

                // trova il massimo valore di x
                if (maxX < c.x){
                    maxX = c.x;
                }

                _Coordinates.add(c);
            } catch (Exception ex) {
                Log.e(Constants.BT_LOG, "error getting coordinate couple");
            }
        }

        // disegna il grafico
        try {
            XYSeries series = new XYSeries("test");
            for (int i = 0; i < _Coordinates.size(); i++) {
                series.add(_Coordinates.get(i).x, _Coordinates.get(i).y);
            }

            // Now we create the renderer
            XYSeriesRenderer renderer = new XYSeriesRenderer();
            renderer.setLineWidth(2);
            renderer.setColor(Color.RED);
            // Include low and max value
            renderer.setDisplayBoundingPoints(true);
            // we add point markers
            renderer.setPointStyle(PointStyle.CIRCLE);
            renderer.setPointStrokeWidth(3);

            XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
            mRenderer.addSeriesRenderer(renderer);

            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            dataset.addSeries(series);

            // We want to avoid black border
            mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
            // Disable Pan on two axis
            //mRenderer.setPanEnabled(false, false);
            //mRenderer.setYAxisMax(200);
            mRenderer.setYAxisMin(0);
            mRenderer.setShowGrid(true); // we show the grid

            GraphicalView chartView = ChartFactory.getLineChartView(this, dataset, mRenderer);

            // aggiungi il grafico alla vista e visualizzalo, nascondi la scritta di attesa
            if (chartLyt != null) {
                llWaitBar.setVisibility(View.GONE);
                chartLyt.setVisibility(View.VISIBLE);
                chartLyt.addView(chartView, 0);
            }
        }
        catch (Exception ex){
            Log.e(Constants.BT_LOG, ex.getMessage());
        }
        finally {
            unlockScreenOrientation();
            creating = false;
        }
    }


    // BLUETOOTH MANAGEMENT

    public void requestData(View view){
        lockScreenOrientation();

        chartLyt.setVisibility(View.GONE);
        llWaitBar.setVisibility(View.VISIBLE);

        // richiedi l'invio dei dati al dispositivo
        if (_Coordinates == null){
            _Coordinates = new ArrayList<Coordinates>();
        }

        // resetto le i dati del ciclo precedente
        received = "";
        _Coordinates.clear();
        _Count = 0;

        // invio della A per avvio
        String message = "A";
        sendMessage(message);

        // IDEA:
        // invio della data attuale
        //SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        //String message = String.format("D%sA", sdf.format(new Date()));
        /*
        * D: avvio del ciclo di lettura della data
        * 19 caratteri di data
        * A: avvio del ciclo di misurazione
        * */
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Resources resources = getResources();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to)+ " " + mConnectedDeviceName, resources.getColor(R.color.blue));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(getString(R.string.title_connecting), resources.getColor(R.color.green));
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(getString(R.string.title_not_connected), resources.getColor(R.color.red));
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    // TODO        show sent data
                    break;
                case Constants.MESSAGE_READ:
                    _Count++;
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.d(Constants.BT_LOG, readMessage);
                    // TODO        received data
                    if (readMessage != null) {
                        if (tvResponse != null) {
                            tvResponse.setText(String.valueOf(_Count));
                        }

                        received += readMessage;

                        if (received.endsWith("S")) {
                            // fermati e disegna il grafico
                            mChatService.stop();
                            if (mainHandler != null) {
                                mainHandler.postDelayed(uiRunnable, 100);
                            }
                        }
                    }

                    // test di ricezione coordinate giro per giro
                    /*if (_Coordinates.size() > 0 && _Coordinates.size() % 10 == 0){
                        mChatService.stop();
                        if (mainHandler != null && !creating){
                                mainHandler.postDelayed(uiRunnable, 100);
                        }
                    }*/

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != _me) {
                        Toast.makeText(_me, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != _me) {
                        Toast.makeText(_me, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    // chart drawing handler
    Handler uiHandler = new Handler(Looper.getMainLooper());
    Handler mainHandler = new Handler();
    Runnable uiRunnable = new Runnable() {
        @Override
        public void run() {
            Thread one=new Thread(){
                public void run() {
                    uiHandler.postDelayed(chartRunnable, 200);
                }
            };
            one.start();
        }
    };
    Runnable chartRunnable = new Runnable() {
        public void run() {
            drawChart();
        }
    };

}
