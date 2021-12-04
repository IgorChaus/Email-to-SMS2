package com.example.email_to_sms2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {

        String email =  intent.getStringExtra("email");
        String password = intent.getStringExtra("password");
        String smtp_server = intent.getStringExtra("smtp_server");
        String port = intent.getStringExtra("port");

        Log.i("MyTag","onReceive ");

        Intent i = new Intent(context,MyJobIntentService.class);

        i.putExtra("email",email);
        i.putExtra("password",password);
        i.putExtra("smtp_server",smtp_server);
        i.putExtra("port",port);

        MyJobIntentService.enqueueWork(context, i);
    }
}

