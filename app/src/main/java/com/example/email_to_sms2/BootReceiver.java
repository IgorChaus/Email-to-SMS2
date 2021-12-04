package com.example.email_to_sms2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

public class BootReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    @Override
    public void onReceive(Context context, Intent intent) {
        long time_interval;

        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(context);
        String email = sharePref.getString("Email","");
        String password = sharePref.getString("Password","");
        String smtp_server = sharePref.getString("Smtp_server","");
        String port = sharePref.getString("Port","");
        Boolean alarmUp = sharePref.getBoolean("alarmUp",false);

        switch (sharePref.getString("time","Каждый час")) {
            case ("Каждый час"):
                time_interval = AlarmManager.INTERVAL_HOUR;
                break;
            case ("Каждый 15 минут"):
                time_interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                break;
            case ("Каждый 30 минут"):
                time_interval = AlarmManager.INTERVAL_HALF_HOUR;
                break;
            default:
                time_interval = AlarmManager.INTERVAL_HOUR;
                break;
        }


        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent BootIntent = new Intent(context, MyBroadcastReceiver.class);

        BootIntent.putExtra("email",email);
        BootIntent.putExtra("password",password);
        BootIntent.putExtra("smtp_server",smtp_server);
        BootIntent.putExtra("port", port);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, BootIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmUp) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    time_interval,
                    pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }

    }
}
