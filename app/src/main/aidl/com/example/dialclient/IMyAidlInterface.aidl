// IMyAidlInterface.aidl
package com.example.dialclient;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
   String SendData();
    void ReceiveData(int offset,String command,int port,String ipaddress );
}