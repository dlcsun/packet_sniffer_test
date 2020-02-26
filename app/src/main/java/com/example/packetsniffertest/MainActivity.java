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
import android.os.Environment;
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
import java.io.OutputStreamWriter;
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
        whitelist = new ArrayList<>(Arrays.asList("z05","BB-1A48","20003","20004"));
        b1id = "z05";
        b2id = "BB-1A48";
        lastb1 = lastb2 = 1;
        lastb1time = lastb2time = currenttime = LocalTime.now();
        b1available = b2available = false;
        usewhitelist = true;
        datacount = 0;
        currentfilename = "";

        final Button button = findViewById(R.id.button);
        final TextView tv = findViewById(R.id.textView);
        final TextView tv2 = findViewById(R.id.textView2);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final ScanCallback callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                currenttime = LocalTime.now();
                String beaconid = result.getDevice().getName();
                super.onScanResult(callbackType, result);

                if (!btids.contains(result.getDevice().getAddress()) &&
                        (whitelist.contains(beaconid) || !usewhitelist) ){
                    Log.d("Scan", "Found a new bluetooth signal");
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %s\n",
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

                    if (Objects.equals(beaconid, b1id)){
                        Log.d("initial set", "saw b1id");
                        if(currenttime.isBefore(lastb2time.plusSeconds(1)) && b2available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    lastb2time.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            tv2.setText(String.valueOf(datacount));
                            b1available = false;
                            b2available = false;
                            Log.d("update", "matching pair with last b2 (" + String.valueOf(lastb1) + ", " + String.valueOf(lastb2) + ")");
                        }
                        else{
                            lastb1 = result.getRssi();
                            lastb1time = currenttime;
                            b1available = true;
                            Log.d("initial set", "no match; setting b1available to true; b1 = " + String.valueOf(lastb1) + "*; b2 = " + String.valueOf(lastb2));
                        }
                    }
                    else if (Objects.equals(beaconid, b2id)){
                        Log.d("initial set", "saw b2id");
                        if(currenttime.isBefore(lastb1time.plusSeconds(1)) && b1available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    lastb1time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            tv2.setText(String.valueOf(datacount));
                            b1available = false;
                            b2available = false;
                            Log.d("initial set", "matching pair with last b1 (" + String.valueOf(lastb1) + ", " + String.valueOf(lastb2) + ")");
                        }
                        else{
                            lastb2 = result.getRssi();
                            lastb2time = currenttime;
                            b2available = true;
                            Log.d("initial set", "no match; setting b2available to true; b1 = " + String.valueOf(lastb1) + "; b2 = " + String.valueOf(lastb2) + "*");
                        }
                    }
                }
                else if (btids.contains(result.getDevice().getAddress())){
                    Log.d("Scan", "Found a duplicate bluetooth signal");
                    //update last seen
                    int loc = btids.indexOf(result.getDevice().getAddress());
                    String parsedResult = String.format(
                            "Name: %-22s | Address: %s | RSSI: %-3d | %s\n",
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

                    if (Objects.equals(beaconid, b1id)){
                        Log.d("update", "saw b1id");
                        if(currenttime.isBefore(lastb2time.plusSeconds(1)) && b2available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    lastb2time.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            tv2.setText(String.valueOf(datacount));
                            b1available = false;
                            b2available = false;
                            Log.d("update", "matching pair with last b2 (" + String.valueOf(lastb1) + ", " + String.valueOf(lastb2) + ")");
                        }
                        else{
                            lastb1 = result.getRssi();
                            lastb1time = currenttime;
                            b1available = true;
                            Log.d("update", "no match; setting b1available to true; b1 = " + String.valueOf(lastb1) + "*; b2 = " + String.valueOf(lastb2));
                        }
                    }
                    else if (Objects.equals(beaconid, b2id)){
                        Log.d("update", "saw b2id");
                        if(currenttime.isBefore(lastb1time.plusSeconds(1)) && b1available) {
                            String data = String.format(
                                    "%d %d %s %s",
                                    lastb1,
                                    lastb2,
                                    lastb1time.format(DateTimeFormatter.ISO_LOCAL_TIME),
                                    currenttime.format(DateTimeFormatter.ISO_LOCAL_TIME)
                            );
                            writeToFile(data, currentfilename, getApplicationContext());
                            datacount++;
                            tv2.setText(String.valueOf(datacount));
                            b1available = false;
                            b2available = false;
                            Log.d("update", "matching pair with last b1 (" + String.valueOf(lastb1) + ", " + String.valueOf(lastb2) + ")");
                        }
                        else{
                            lastb2 = result.getRssi();
                            lastb2time = currenttime;
                            b2available = true;
                            Log.d("update", "no match; setting b2available to true; b1 = " + String.valueOf(lastb1) + "; b2 = " + String.valueOf(lastb2) + "*");
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
                        tv2.setText("0");
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
                                    "Name: %-22s | Address: %s | RSSI: %-3d | %s\n",
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
        //File path = context.getFilesDir();
        //File file = new File(path, filename);
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        File file = new File(path, filename);
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            //Log.d("Writing", "file location: " + file.getAbsolutePath());
            //stream.write(data.getBytes());
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.append(data + '\n');
            writer.close();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
