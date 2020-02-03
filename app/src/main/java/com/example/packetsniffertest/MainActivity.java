package com.example.packetsniffertest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> btids;
    ArrayList<String> wfids;
    ArrayList<String> parsedResults;
    ArrayList<String> whitelist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Context context = getApplicationContext();
        //BluetoothManager btm = (BluetoothManager)context.getSystemService(BLUETOOTH_SERVICE);
        //final BluetoothAdapter bta = btm.getAdapter();
        final BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        final WifiManager wfm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        registerReceiver(br, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        btids = new ArrayList<>();
        parsedResults = new ArrayList<>();
        whitelist = new ArrayList<>(Arrays.asList("20001","20002","20003","20004"));

        final Button button = findViewById(R.id.button);
        final TextView tv = findViewById(R.id.textView);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (!btids.contains(result.getDevice().getAddress()) &&
                        whitelist.contains(result.getDevice().getUuids())) {
                    //Log.d("Scan", "Found a new bluetooth signal");
                    Calendar calendar = Calendar.getInstance();
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %02d:%02d:%02d\n",
                            result.getDevice().getName(),
                            result.getDevice().getAddress(),
                            result.getRssi(),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            calendar.get(Calendar.SECOND));
                    parsedResults.add(parsedResult);
                    StringBuilder out = new StringBuilder();
                    for (String s:parsedResults) {
                        out.append(s);
                    }
                    tv.setText(out.toString());
                    btids.add(result.getDevice().getAddress());
                }
                else {
                    //Log.d("Scan", "Found a duplicate bluetooth signal");
                    //update last seen
                    int loc = btids.indexOf(result.getDevice().getAddress());
                    Calendar calendar = Calendar.getInstance();
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %02d:%02d:%02d\n",
                            result.getDevice().getName(),
                            result.getDevice().getAddress(),
                            result.getRssi(),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            calendar.get(Calendar.SECOND));
                    parsedResults.set(loc, parsedResult);
                    StringBuilder out = new StringBuilder();
                    for (String s:parsedResults) {
                        out.append(s);
                    }
                    tv.setText(out.toString());
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d("Scan","Found a batch of signals");
                StringBuilder update = new StringBuilder();
                for (ScanResult result:results) {
                    String parsedResult = String.format(
                            "Name: %s$50 RSSI: %d\n",
                            result.getDevice().getName(),
                            result.getRssi());
                    update.append(parsedResult);
                }
                tv.setText(update.toString());
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d("Scan","Could not start scan");
            }
        };

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch sw = findViewById(R.id.switch1);
                if(sw.isChecked()) {
                    BluetoothLeScanner bts = bta.getBluetoothLeScanner();
                    if (button.getText() == getString(R.string.buttonstarttext)) {
                        sw.setEnabled(false);
                        tv.setText("");
                        button.setText(R.string.buttonactivetext);
                        bts.startScan(callback);
                    } else {
                        sw.setEnabled(true);
                        button.setText(R.string.buttonstarttext);
                        bts.stopScan(callback);
                    }
                }
                else {
                    if (button.getText() == getString(R.string.buttonstarttext)) {
                        sw.setEnabled(false);
                        tv.setText("");
                        button.setText(R.string.buttonactivetext);
                        wfm.startScan();
                    } else {
                        sw.setEnabled(true);
                        button.setText(R.string.buttonstarttext);
                        List<android.net.wifi.ScanResult> results = wfm.getScanResults();
                        StringBuilder update = new StringBuilder();
                        Calendar calendar = Calendar.getInstance();
                        for (android.net.wifi.ScanResult result:results) {
                            String parsedResult = String.format(
                                    "Name: %-22s | Address: %s | RSSI: %-3d | %02d:%02d:%02d\n",
                                    result.SSID,
                                    result.BSSID,
                                    result.level,
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    calendar.get(Calendar.SECOND));
                            update.append(parsedResult);
                        }
                        tv.setText(update.toString());
                    }
                }
            }
        });
    }


}
