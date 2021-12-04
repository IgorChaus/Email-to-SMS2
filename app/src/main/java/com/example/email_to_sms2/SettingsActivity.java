package com.example.email_to_sms2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    Button button;
    private EditText email, password, smtp_server, port, time;
    Boolean alarmUp;
    View view;
    SharedPreferences sharePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
  //      ActionBar actionBar = getSupportActionBar();

        button = findViewById(R.id.button);

        sharePref = PreferenceManager.getDefaultSharedPreferences(this);
        alarmUp = sharePref.getBoolean("alarmUpm",false);

        Intent intent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);//the same as up
        alarmUp = (PendingIntent.getBroadcast(getBaseContext(), MyBroadcastReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            button.setText("Stop");
        }else {
            button.setText("Start");
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    //        setPreferencesFromResource(R.xml.root_preferences, rootKey);
            addPreferencesFromResource(R.xml.root_preferences);
        }
    }

    public void onClickButton(View view) {
        SharedPreferences.Editor editor = sharePref.edit();
        if (!alarmUp) {
            button.setText("Stop");
            alarmUp = true;
            editor.putBoolean("alarmUp",alarmUp);
            startAlert();

        } else {
            button.setText("Start");
            alarmUp = false;
            editor.putBoolean("alarmUp",alarmUp);
            stopAlert();
        }
        editor.apply();
    }

    public void startAlert(){
        long time_interval;

        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        intent.putExtra("email",sharePref.getString("email",""));
        intent.putExtra("password",sharePref.getString("password",""));
        intent.putExtra("smtp_server",sharePref.getString("server",""));
        intent.putExtra("port",sharePref.getString("port",""));
        Log.i("MyTag","Time " + sharePref.getString("time","Каждый час"));
        switch (sharePref.getString("time","Каждый час")) {
            case ("Каждый час"):
                time_interval = AlarmManager.INTERVAL_HOUR;
                break;
            case ("Каждый 15 минут"):
                time_interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                break;
            case ("Каждые 30 минут"):
                time_interval = AlarmManager.INTERVAL_HALF_HOUR;
                break;
            default:
                time_interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
                break;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), MyBroadcastReceiver.REQUEST_CODE, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(), time_interval ,pendingIntent);

        Log.i("MyTag","Started");
    }

    public void stopAlert(){
        Intent intent = new Intent(this, MyBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),MyBroadcastReceiver.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }


}