package com.example.dialclient;

import static com.example.dialclient.MainActivity.device_name_Edit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.sql.Struct;

public class IPCReceiver extends BroadcastReceiver {

    public static String UDP_Data;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Test++", "Received : " + intent.getStringExtra("data"));
        Toast.makeText(context, intent.getStringExtra("data"), Toast.LENGTH_SHORT).show();

        MainActivity.isudpdatareceived = true;
        UDP_Data = intent.getStringExtra("data");



    }
}