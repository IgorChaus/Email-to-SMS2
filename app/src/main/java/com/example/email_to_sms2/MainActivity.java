package com.example.email_to_sms2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Button button;
    private EditText email, password, smtp_server, port, time;
    Boolean alarmUp;
    View view;
    SharedPreferences sharePref;
    public PeriodicWorkRequest uploadWorkRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        sharePref = PreferenceManager.getDefaultSharedPreferences(this);

        //    проверяем запущен ли сервис
        alarmUp = isWorkScheduled("mytag");
        if (alarmUp == true) {
            button.setText("Stop");
        } else {
            button.setText("Start");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        return super.onOptionsItemSelected(item);
    }

    public void onClickButton(View view) {
        if (!alarmUp) {
            button.setText("Stop");
            alarmUp = true;
            startAlert();

        } else {
            button.setText("Start");
            alarmUp = false;
            stopAlert();
        }
    }

    public void startAlert(){
        long time_interval;

        /*Data myData = new Data.Builder()
                .putString("email", sharePref.getString("email",""))
                .putString("password", sharePref.getString("password",""))
                .putString("smtp_server", sharePref.getString("server",""))
                .putString("port", sharePref.getString("port",""))
                .build();*/

        Data myData = new Data.Builder()
                .putString("email", "alarm-parking@mail.ru")
                .putString("password", "chusya12")
                .putString("smtp_server", "imap.mail.ru")
                .putString("port", "995")
                .build();

       /* switch (sharePref.getString("time","Каждые 15 минут")) {
            case ("Каждый час"):
                time_interval = 60;
                break;
            case ("Каждые 15 минут"):
                time_interval = 15;
                break;
            case ("Каждые 30 минут"):
                time_interval = 30;
                break;
            default:
                time_interval = 15;
        }*/

        time_interval = 15;

        uploadWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, time_interval, TimeUnit.MINUTES)
                .setInputData(myData).addTag("mytag").build();
        WorkManager.getInstance(this.getApplicationContext()).enqueue(uploadWorkRequest);

    }

    public void stopAlert(){
        WorkManager.getInstance(this.getApplicationContext()).cancelAllWorkByTag("mytag");
    }

    //Функция проверяет запущена ли уже задача
    private boolean isWorkScheduled(String tag) {
        WorkManager instance = WorkManager.getInstance(this.getApplicationContext());
        ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosByTag(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED;
            }
            return running;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
