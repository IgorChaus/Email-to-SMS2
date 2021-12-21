package com.example.email_to_sms2;

import static com.example.email_to_sms2.DBHelper.KEY_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;
    private EditText email, password, smtp_server, port, time;
    Boolean alarmUp, smsPermission;
    View view;
    SharedPreferences sharePref;
    public PeriodicWorkRequest uploadWorkRequest;
    DBHelper dbHelper;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);
        sharePref = PreferenceManager.getDefaultSharedPreferences(this);

        //Проверяем разрешение на отправку SMS
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            // Permission not yet granted. Use requestPermissions().
            // MY_PERMISSIONS_REQUEST_SEND_SMS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            smsPermission = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            // Permission already granted. Enable the SMS button.
            smsPermission = true;
        }


        ReadLog();

        DataRepository.getData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (textView.getText().toString() == "Таблица логов пустая") {
                    textView.setText(s);
                }else {
                    textView.append(s);
                }
            }

        });

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

        if (smsPermission) {

            if (!alarmUp) {
                ReadLog();
                button.setText("Stop");
                alarmUp = true;
                startAlert();

            } else {
                button.setText("Start");
                alarmUp = false;
                stopAlert();
            }
        }else {
            Toast.makeText(this,
                    "Выдайте разрешение для SMS",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void startAlert(){
        long time_interval;

        Data myData = new Data.Builder()
                .putString("email", sharePref.getString("email",""))
                .putString("password", sharePref.getString("password",""))
                .putString("smtp_server", sharePref.getString("server",""))
                .putString("port", sharePref.getString("port",""))
                .putString("message_action",sharePref.getString("message_action","Помечать прочитанными"))
                .putString("token",sharePref.getString("token","1111"))
                .build();

        switch (sharePref.getString("time","Каждые 15 минут")) {
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
        }


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

    private void ReadLog(){
        String len_log = sharePref.getString("len_log","10");

        textView.setText("");

        dbHelper = new DBHelper(this);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
//        Cursor cursor = database.query(DBHelper.TABLE_MESSAGE, null, null, null, null, null, null);
        Cursor cursor = database.query(DBHelper.TABLE_MESSAGE, null, null, null, null, null, KEY_ID + " DESC",len_log);

        if (cursor.moveToLast()) {
            int datatimeIndex = cursor.getColumnIndex(DBHelper.KEY_DATATIME);
            int typeIndex = cursor.getColumnIndex(DBHelper.KEY_TYPE);
            int messageIndex = cursor.getColumnIndex(DBHelper.KEY_MESSAGE);
            do {
                textView.append(cursor.getString(datatimeIndex)+ " " +
                        cursor.getString(messageIndex) + System.getProperty("line.separator"));

            } while (cursor.moveToPrevious());
        } else
            textView.setText("Таблица логов пустая");

        cursor.close();
        dbHelper.close();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (permissions[0].equalsIgnoreCase
                        (Manifest.permission.SEND_SMS)
                        && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted. Enable sms button.
                    smsPermission = true;
                } else {
                    // Permission denied.
                    smsPermission = false;;
                }
            }
        }

    }


}
