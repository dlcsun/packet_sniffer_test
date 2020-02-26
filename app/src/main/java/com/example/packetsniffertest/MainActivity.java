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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> btids;
    ArrayList<String> wfids;
    ArrayList<String> parsedResults;
    ArrayList<String> whitelist;
    String b1id;
    String b2id;
    LocalTime lastb1time;
    LocalTime lastb2time;
    LocalTime currenttime;
    int lastb1;
    int lastb2;
    int datacount;
    boolean b1available;
    boolean b2available;
    boolean usewhitelist;
    String currentfilename;

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
        b1id = "20001";
        b2id = "20002";
        lastb1 = lastb2 = 1;
        lastb1time = lastb2time = currenttime = LocalTime.now();
        b1available = b2available = false;
        usewhitelist = false;
        datacount = 0;
        currentfilename = "";

        final Button button = findViewById(R.id.button);
        final TextView tv = findViewById(R.id.textView);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                currenttime = LocalTime.now();
                String uuid1 = result.getDevice().getUuids()[0].toString().substring(0,5);
                super.onScanResult(callbackType, result);
                Log.d("Scan", "Found a new bluetooth signal");
                if (!btids.contains(result.getDevice().getAddress()) &&
                        (whitelist.contains(uuid1) || !usewhitelist) ){
                    //Log.d("Scan", "Found a new bluetooth signal");
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %02d\n",
                            result.getDevice().getName(),
                            result.getDevice().getAddress(),
                            result.getRssi(),
                            currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME));
                    parsedResults.add(parsedResult);
                    StringBuilder out = new StringBuilder();
                    for (String s:parsedResults) {
                        out.append(s);
                    }
                    tv.setText(out.toString());
                    btids.add(result.getDevice().getAddress());

                    if (Objects.equals(uuid1, b1id)){
                        if(currenttime.isBefore(lastb2time.plusNanos(500)) && b2available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    lastb2time.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            b1available = false;
                            b2available = false;
                        }
                        else{
                            lastb1 = result.getRssi();
                            lastb1time = currenttime;
                            b1available = true;
                        }
                    }
                    else if (Objects.equals(uuid1, b2id)){
                        if(currenttime.isBefore(lastb1time.plusNanos(500)) && b1available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    lastb1time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            b1available = false;
                            b2available = false;
                        }
                        else{
                            lastb2 = result.getRssi();
                            lastb2time = currenttime;
                            b2available = true;
                        }
                    }
                }
                else {
                    //Log.d("Scan", "Found a duplicate bluetooth signal");
                    //update last seen
                    int loc = btids.indexOf(result.getDevice().getAddress());
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %02d\n",
                            result.getDevice().getName(),
                            result.getDevice().getAddress(),
                            result.getRssi(),
                            currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME));
                    parsedResults.set(loc, parsedResult);
                    StringBuilder out = new StringBuilder();
                    for (String s:parsedResults) {
                        out.append(s);
                    }
                    tv.setText(out.toString());

                    if (Objects.equals(uuid1, b1id)){
                        if(currenttime.isBefore(lastb2time.plusNanos(500)) && b2available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    lastb2time.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            b1available = false;
                            b2available = false;
                        }
                        else{
                            lastb1 = result.getRssi();
                            lastb1time = currenttime;
                            b1available = true;
                        }
                    }
                    else if (Objects.equals(uuid1, b2id)){
                        if(currenttime.isBefore(lastb1time.plusNanos(500)) && b1available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    lastb1time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            b1available = false;
                            b2available = false;
                        }
                        else{
                            lastb2 = result.getRssi();
                            lastb2time = currenttime;
                            b2available = true;
                        }
                    }
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
                        datacount = 0;
                        currentfilename = String.format(
                                "BTdata(%s).txt",
                                LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss")));
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
                        for (android.net.wifi.ScanResult result:results) {
                            String parsedResult = String.format(
                                    "Name: %-22s | Address: %s | RSSI: %-3d | %02d:%02d:%02d\n",
                                    result.SSID,
                                    result.BSSID,
                                    result.level,
                                    LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
                            update.append(parsedResult);
                        }
                        tv.setText(update.toString());
                    }
                }
            }
        });
    }
    private void writeToFile(String data,String filename,Context context) {
        File path = context.getFilesDir();
        File file = new File(path, filename);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            assert stream != null;
            stream.write(data.getBytes());
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
