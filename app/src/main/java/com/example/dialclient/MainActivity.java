package com.example.dialclient;


import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private DatagramSocket UDPSocket;

    private InetAddress address;

    private int offset = 1;

    private String ipaddress = "192.168.100.1";

    //private String ipaddress = "192.168.0.126";

    // private String ipaddress = "192.168.0.141";

    private int port = 5010;

    EditText edtPhoneNo;

    static TextView ReceiveMessage;

    private long startTime = 0L;

    private Handler customHandler = new Handler();

    long timeInMilliseconds = 0L;

    long timeSwapBuff = 0L;

    long updatedTime = 0L;

    boolean notificationAcceptclick = false;

    ToneGenerator mToneGenerator = null;

    int DialVolume = 80;

    String[] parts;

    String part1;

    String part2;

    Ringtone incomingRingTone;

    MediaPlayer outGoinRingTone;

    TextView mute, Unmute;

    Button btndel;

    public static EditText device_name_Edit;

    CheckBox device_name_check;

    LinearLayout makecalllayout, endcalllayout;

    AlertDialog.Builder builder;

    AlertDialog alertDialog;

    Boolean Incoming = false;

    private Context mContext;

    String Rxfromservice;

    public static boolean isudpdatareceived = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        //Linear layout
        makecalllayout = (LinearLayout) findViewById(R.id.makecalllayout);
        endcalllayout = (LinearLayout) findViewById(R.id.endcalllayout);
        //TextView
        ReceiveMessage = (TextView) findViewById(R.id.ReceiveMessage);
        mute = (TextView) findViewById(R.id.mute);
        Unmute = (TextView) findViewById(R.id.Unmute);
        //Button
        btndel = (Button) findViewById(R.id.btndel);
        //Edit text
        device_name_Edit = findViewById(R.id.device_name_Edit);
        edtPhoneNo = (EditText) findViewById(R.id.edtPhoneNumber);
        //Check box
        device_name_check = findViewById(R.id.device_name_check);
        //incoming Call ringing tone
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, DialVolume);
        //outgoing ringtone
        outGoinRingTone = MediaPlayer.create(MainActivity.this, R.raw.ring);
        //Alert dialog
        builder = new AlertDialog.Builder(MainActivity.this);
        //Customize action bar
        this.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);


        /*Edit device name check box*/
        device_name_check.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                device_name_Edit.setEnabled(true);
                device_name_Edit.requestFocus();
            } else {
                device_name_Edit.setEnabled(false);
                device_name_Edit.getText().clear();
            }
        });


        Thread updateuithread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (isudpdatareceived) {
                            fnupdateUI(IPCReceiver.UDP_Data);
                            isudpdatareceived = false;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        updateuithread.start();

        senddatatoserverapp("GET_NAME#");


    }


    public void fnupdateUI(String str) {


        if (str.trim().toUpperCase().equals("ACCEPT")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!Incoming) {
                        outGoinRingTone.setLooping(false);
                        outGoinRingTone.stop();
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    } else {
                        Incoming = false;
                        makecalllayout.setVisibility(View.GONE);
                        btndel.setVisibility(View.GONE);
                        endcalllayout.setVisibility(View.VISIBLE);
                        incomingRingTone.stop();
                        alertDialog.dismiss();
                        notificationAcceptclick = true;
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    }


                }
            });
            /*Reject the call using this below response*/
        } else if (str.trim().toUpperCase().equals("REJECT")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makecalllayout.setVisibility(View.VISIBLE);
                    btndel.setVisibility(View.VISIBLE);
                    endcalllayout.setVisibility(View.GONE);
                    outGoinRingTone.setLooping(false);
                    outGoinRingTone.stop();
                    customHandler.removeCallbacks(updateTimerThread);
                    ReceiveMessage.setText("call ended");
                    edtPhoneNo.setText("");

                }
            });
            /*Cut the call using this below response*/
        } else if (str.trim().toUpperCase().equals("CLOSE")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!Incoming) {
                        makecalllayout.setVisibility(View.VISIBLE);
                        btndel.setVisibility(View.VISIBLE);
                        endcalllayout.setVisibility(View.GONE);
                        outGoinRingTone.stop();
                        customHandler.removeCallbacks(updateTimerThread);
                        ReceiveMessage.setText("call ended");
                        edtPhoneNo.setText("");
                    } else {
                        Incoming = false;
                        alertDialog.dismiss();
                        incomingRingTone.stop();
                        customHandler.removeCallbacks(updateTimerThread);
                        ReceiveMessage.setText("call ended");

                    }


                }
            });
            /*Dial from connected device using this below response*/
        } else if (str.trim().toUpperCase().equals("DIAL")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makecalllayout.setVisibility(View.GONE);
                    btndel.setVisibility(View.GONE);
                    endcalllayout.setVisibility(View.VISIBLE);
                    ReceiveMessage.setText("Dialling from connected device ");
                    outGoinRingTone = MediaPlayer.create(MainActivity.this, R.raw.ring);
                    outGoinRingTone.setLooping(true);
                    outGoinRingTone.start();


                }
            });
        } else if (str.trim().toUpperCase().equals("INCOMING##")
                || str.trim().toUpperCase().equals("INCOMING#0#")) {

        } else {
            parts = str.split("#");
            part1 = parts[0];
            part2 = parts[1];
            Log.d("Data Received ", part1);
            Log.d("Data Received", part2);
            /*Incoming call using this below response*/
            if (part1.toUpperCase().equals("INCOMING")) {
                Incoming = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                        incomingRingTone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        incomingRingTone.play();


                        //builder.setMessage("Data Received : " + new String(finalStr));
                        builder.setMessage(part2);
                        builder.setTitle("Incoming call ");
                        builder.setCancelable(false);
                        builder.setPositiveButton("Accept", (DialogInterface.OnClickListener) (dialog, which) -> {
                            /*Incoming call Accept command*/
                            senddatatoserverapp("ACCEPT#");
                            makecalllayout.setVisibility(View.GONE);
                            btndel.setVisibility(View.GONE);
                            endcalllayout.setVisibility(View.VISIBLE);
                            incomingRingTone.stop();
                            notificationAcceptclick = true;
                            startTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimerThread, 0);
                        });
                        builder.setNegativeButton("Reject", (DialogInterface.OnClickListener) (dialog, which) -> {
                            incomingRingTone.stop();
                            /*Incoming call Reject command*/
                            senddatatoserverapp("REJECT#");
                            //SendData(offset, "REJECT#", port, ipaddress);
                            customHandler.removeCallbacks(updateTimerThread);
                            ReceiveMessage.setText("call ended");
                            dialog.cancel();
                        });
                        alertDialog = builder.create();
                        alertDialog.show();
                    }
                });
                /*Get the device name using this below response*/
            } else if (part1.toUpperCase().equals("NAME")) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        device_name_Edit.setText(part2);
                    }
                });
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    /*all buutons integrations*/
    public void buttonClickEvent(View v) {
        String phoneNo = edtPhoneNo.getText().toString();
        try {
            switch (v.getId()) {
                /*Get button*/
                case R.id.btnGet:

                    senddatatoserverapp("GET_NAME#");

                    break;
                /*Set button */
                case R.id.btnSet:
                    /*Set button command*/
                    if (device_name_Edit.getText().toString().isEmpty()) {
                        ReceiveMessage.setText("Please enter the device name");
                    } else {
                        senddatatoserverapp("SET_NAME#");
                    }
                    break;
                /*Pair button */
                case R.id.btnPair:
                    /*Pair button command*/
                    senddatatoserverapp("PAIR#");
                    break;

                /*UnPair button */
                case R.id.btnUnpair:
                    senddatatoserverapp("UNPAIR#");
                    break;

                case R.id.btnwebview:
                    Fragment fragment = new webview();
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.frameLayout, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    break;

                /*Audio on button */
                case R.id.btnAudioOn:
                    /*Audio on button command*/
                    senddatatoserverapp("AUDIO_ON#");
                    break;
                /*Audio off button */
                case R.id.btnAudioOff:
                    senddatatoserverapp("AUDIO_OFF#");
                    /*Audio off button command*/
                    break;

                /*Star button */
                case R.id.btnAterisk:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "*";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Hash button */
                case R.id.btnHash:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "#";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*zero button */
                case R.id.btnZero:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "0";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*one button */
                case R.id.btnOne:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "1";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*two button */
                case R.id.btnTwo:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "2";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*three button */
                case R.id.btnThree:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "3";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*four button */
                case R.id.btnFour:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "4";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*five button */
                case R.id.btnFive:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "5";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Six button */
                case R.id.btnSix:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "6";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Seven button */
                case R.id.btnSeven:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "7";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Eight button */
                case R.id.btnEight:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "8";
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Nine button */
                case R.id.btnNine:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("Dialing.....");
                    phoneNo += "9";
                    edtPhoneNo.setText(phoneNo);

                    break;
                /*Delete button */
                case R.id.btndel:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("");
                    if (phoneNo != null && phoneNo.length() > 0) {
                        phoneNo = phoneNo.substring(0, phoneNo.length() - 1);
                    }
                    edtPhoneNo.setText(phoneNo);
                    break;
                /*Clear all button */
                case R.id.btnClearall:
                    mToneGenerator.startTone(ToneGenerator.TONE_DTMF_1, DialVolume);
                    ReceiveMessage.setText("");
                    edtPhoneNo.setText("");

                    break;
                /*mute button */
                case R.id.mute:
                    mute.setVisibility(View.GONE);
                    Unmute.setVisibility(View.VISIBLE);
                    break;
                /*Unmute button */
                case R.id.Unmute:
                    mute.setVisibility(View.VISIBLE);
                    Unmute.setVisibility(View.GONE);
                    break;
                /*Keypad button */
                case R.id.keypad:
                    Toast.makeText(getApplicationContext(), "Keypad button clicked", Toast.LENGTH_SHORT).show();
                    break;
                /*Speaker button */
                case R.id.speaker:
                    Toast.makeText(getApplicationContext(), "Speaker button clicked", Toast.LENGTH_SHORT).show();
                    break;
                /*Add call button */
                case R.id.addcall:
                    Toast.makeText(getApplicationContext(), "Add call button clicked", Toast.LENGTH_SHORT).show();
                    break;
                /*Record button */
                case R.id.record:
                    Toast.makeText(getApplicationContext(), "Record button clicked", Toast.LENGTH_SHORT).show();
                    break;
                /*Cut the call button */
                case R.id.btnEndcall:
                    makecalllayout.setVisibility(View.VISIBLE);
                    btndel.setVisibility(View.VISIBLE);
                    endcalllayout.setVisibility(View.GONE);
                    ReceiveMessage.setText("call ended");
                    edtPhoneNo.setText("");
                    outGoinRingTone.stop();
                    /*Cut the call command */
                    senddatatoserverapp("DISCONNECT#");
                    //SendData(offset, "DISCONNECT#", port, ipaddress);
                    customHandler.removeCallbacks(updateTimerThread);
                    break;
                /*Make call button*/
                case R.id.btnCall:
                    if (phoneNo.trim().equals("")) {
                        ReceiveMessage.setText("Please enter a number to call on!");
                    } else {
                        Boolean isHash = false;
                        if (phoneNo.subSequence(phoneNo.length() - 1, phoneNo.length()).equals("#")) {
                            phoneNo = phoneNo.substring(0, phoneNo.length() - 1);
                            String callInfo = "tel:" + phoneNo + Uri.encode("#");
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse(callInfo));
                            startActivity(callIntent);
                        } else {
                            String callInfo = "tel:" + phoneNo;
                            makecalllayout.setVisibility(View.GONE);
                            btndel.setVisibility(View.GONE);
                            endcalllayout.setVisibility(View.VISIBLE);
                            ReceiveMessage.setText("Ringing....");
                            /*Make call command*/
                            senddatatoserverapp("DIAL#" + phoneNo + "#");
                            outGoinRingTone = MediaPlayer.create(MainActivity.this, R.raw.ring);
                            outGoinRingTone.setLooping(true);
                            outGoinRingTone.start();
                        }
                    }
                    break;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /*On call timer running functionalities*/
    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            if (notificationAcceptclick) {
                edtPhoneNo.setText(part2);
                ReceiveMessage.setText("on call\n" + "" + mins + ":"
                        + String.format("%02d", secs));
                notificationAcceptclick = false;
            } else {
                ReceiveMessage.setText("on call\n" + "" + mins + ":"
                        + String.format("%02d", secs));
            }
            customHandler.postDelayed(this, 0);
        }

    };

    private void ReceiveMessageFromService() {
        if (Rxfromservice.trim().toUpperCase().equals("ACCEPT")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!Incoming) {
                        outGoinRingTone.setLooping(false);
                        outGoinRingTone.stop();
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    } else {
                        Incoming = false;
                        makecalllayout.setVisibility(View.GONE);
                        btndel.setVisibility(View.GONE);
                        endcalllayout.setVisibility(View.VISIBLE);
                        incomingRingTone.stop();
                        alertDialog.dismiss();
                        notificationAcceptclick = true;
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                    }
                }
            });
            /*Reject the call using this below response*/
        } else if (Rxfromservice.trim().toUpperCase().equals("REJECT")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makecalllayout.setVisibility(View.VISIBLE);
                    btndel.setVisibility(View.VISIBLE);
                    endcalllayout.setVisibility(View.GONE);
                    outGoinRingTone.setLooping(false);
                    outGoinRingTone.stop();
                    customHandler.removeCallbacks(updateTimerThread);
                    ReceiveMessage.setText("call ended");
                    edtPhoneNo.setText("");

                }
            });
            /*Cut the call using this below response*/
        } else if (Rxfromservice.trim().toUpperCase().equals("CLOSE")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!Incoming) {
                        makecalllayout.setVisibility(View.VISIBLE);
                        btndel.setVisibility(View.VISIBLE);
                        endcalllayout.setVisibility(View.GONE);
                        outGoinRingTone.stop();
                        customHandler.removeCallbacks(updateTimerThread);
                        ReceiveMessage.setText("call ended");
                        edtPhoneNo.setText("");
                    } else {
                        Incoming = false;
                        alertDialog.dismiss();
                        incomingRingTone.stop();
                        customHandler.removeCallbacks(updateTimerThread);
                        ReceiveMessage.setText("call ended");

                    }


                }
            });
            /*Dial from connected device using this below response*/
        } else if (Rxfromservice.trim().toUpperCase().equals("DIAL")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    makecalllayout.setVisibility(View.GONE);
                    btndel.setVisibility(View.GONE);
                    endcalllayout.setVisibility(View.VISIBLE);
                    ReceiveMessage.setText("Dialling from connected device ");
                    outGoinRingTone = MediaPlayer.create(MainActivity.this, R.raw.ring);
                    outGoinRingTone.setLooping(true);
                    outGoinRingTone.start();


                }
            });
        } else if (Rxfromservice.trim().toUpperCase().equals("INCOMING##")
                || Rxfromservice.trim().toUpperCase().equals("INCOMING#0#")) {

        } else {
            parts = Rxfromservice.split("#");
            part1 = parts[0];
            part2 = parts[1];
            Log.d("Data Received ", part1);
            Log.d("Data Received", part2);
            /*Incoming call using this below response*/
            if (part1.toUpperCase().equals("INCOMING")) {
                Incoming = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                        incomingRingTone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        incomingRingTone.play();


                        //builder.setMessage("Data Received : " + new String(finalStr));
                        builder.setMessage(part2);
                        builder.setTitle("Incoming call ");
                        builder.setCancelable(false);
                        builder.setPositiveButton("Accept", (DialogInterface.OnClickListener) (dialog, which) -> {
                            /*Incoming call Accept command*/
                            senddatatoserverapp("ACCEPT#");
                            /*try {
                                aidlInterfaceObject.ReceiveData(offset, "ACCEPT#", port, ipaddress);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }*/
                            //SendData(offset, "ACCEPT#", port, ipaddress);
                            makecalllayout.setVisibility(View.GONE);
                            btndel.setVisibility(View.GONE);
                            endcalllayout.setVisibility(View.VISIBLE);
                            incomingRingTone.stop();
                            notificationAcceptclick = true;
                            startTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimerThread, 0);
                        });
                        builder.setNegativeButton("Reject", (DialogInterface.OnClickListener) (dialog, which) -> {
                            incomingRingTone.stop();
                            /*Incoming call Reject command*/
                            senddatatoserverapp("REJECT#");
                          /*  try {
                                aidlInterfaceObject.ReceiveData(offset, "REJECT#", port, ipaddress);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }*/
                            // SendData(offset, "REJECT#", port, ipaddress);
                            customHandler.removeCallbacks(updateTimerThread);
                            ReceiveMessage.setText("call ended");
                            dialog.cancel();
                        });
                        alertDialog = builder.create();
                        alertDialog.show();
                    }
                });
                /*Get the device name using this below response*/
            } else if (part1.toUpperCase().equals("NAME")) {

                runOnUiThread(new Runnable() {
                    @Override

                    public void run() {
                        device_name_Edit.setText(part2);
                    }
                });
            }


        }
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    private void bindAIDLService() {
        //Try catch block is not added during video implementation
        try {
            Intent settingsIntent = new Intent("com.example.dialservice");
            bindService(convertImplicitIntentToExplicitIntent(settingsIntent, mContext), serviceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            Toast.makeText(mContext, "Service App may not be present", Toast.LENGTH_SHORT).show();
            Log.e("AIDL_ERROR", "EXCEPTION CAUGHT: " + e.toString());
            finish();
        }
    }

    public Intent convertImplicitIntentToExplicitIntent(Intent implicitIntent, Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentServices(implicitIntent, 0);
        if (resolveInfoList == null || resolveInfoList.size() != 1) {
            return null;
        }
        ResolveInfo serviceInfo = resolveInfoList.get(0);
        ComponentName component = new ComponentName(serviceInfo.serviceInfo.packageName, serviceInfo.serviceInfo.name);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }


    public void senddatatoserverapp(String send) {
        Intent explicitIntent = new Intent();
        explicitIntent.putExtra("data", send);
        explicitIntent.setClassName("com.example.dialservice", "com.example.dialservice.IPCReceiver");
        sendBroadcast(explicitIntent);
    }
}



