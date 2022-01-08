package com.example.email_to_sms2;

import static com.example.email_to_sms2.DBHelper.KEY_ID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.sun.mail.util.MailConnectException;

import java.io.File;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

public class MainActivity extends AppCompatActivity {

    Button button, button1;
    TextView textView, textLastCheck;
    private EditText email, password, smtp_server, port, time;
    Boolean alarmUp, smsPermission;
    View view;
    Date currentDate;
    DateFormat dateFormat, timeFormat;
    String dateText, timeText;
    SharedPreferences sharePref;
    public PeriodicWorkRequest uploadWorkRequest;
    DBHelper dbHelper;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    private static String CHANNEL_ID = "123";
    private static final int NOTIFY_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        button1 = findViewById(R.id.button1);
        textView = findViewById(R.id.textView);
        textLastCheck = findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());
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

        DataLastCheck.getData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                textLastCheck.setText(s);
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
        int id = item.getItemId();
        switch(id) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.clear_log:
                clear_log();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onStartButton(View view) {

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

    public void onCheckButton(View view1){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(runnable1);

                 String user = sharePref.getString("email","");
                 String pass = sharePref.getString("password","");
                 String host = sharePref.getString("server","");
                 String port_email = sharePref.getString("port","");
                 String message_action = sharePref.getString("message_action","Помечать прочитанными");
                 String token = sharePref.getString("token","1111");
                 check(user, pass, host, port_email, token, message_action);

                runOnUiThread(runnable2);
            }
        });
        t.start();

    }

    Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            button1.setEnabled(false);
        }
    };

    Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            button1.setEnabled(true);
        }
    };

    void clear_log(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Удаление Лога");
        builder.setMessage("Вы собираетесь удалить Лог приложения. Его удаление повлечет за собой " +
                "удаление всей истории отправленных сообщений."  + System.getProperty("line.separator") +
                System.getProperty("line.separator") + "Вы уверены?");

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                    dbHelper = new DBHelper(getApplicationContext());
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    db.delete(DBHelper.TABLE_MESSAGE, null, null);

                    textView.setText("");
            }
        });
        builder.show();

    }

    void check(String user, String password, String host, String port, String token, String message_action) {

        // Текущее время
        currentDate = new Date();
        // Форматирование времени как "день.месяц.год"
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        dateText = dateFormat.format(currentDate);
        // Форматирование времени как "часы:минуты:секунды"
        timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeText = timeFormat.format(currentDate);
        DataLastCheck.updateText("Последняя проверка была " + dateText + " в " + timeText);

        dbHelper = new DBHelper(getApplicationContext());
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        // Подготавливаем уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_stat_sync);
        builder.setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        try {

            //create properties field
            Properties properties = new Properties();

            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", port);
            properties.put("mail.imap.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the IMAP store object and connect with the IMAP server
            Store store = emailSession.getStore("imaps");
            DataRepository.updateText(System.getProperty("line.separator") + "Connect with " + user + "..." + System.getProperty("line.separator"));
            store.connect(host, user, password);
            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            //    emailFolder.open(Folder.READ_ONLY);
            emailFolder.open(Folder.READ_WRITE);

            // retrieve the messages from the folder in an array and print it
            //   Message[] messages = emailFolder.getMessages();
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = emailFolder.search(ft);

            //   System.out.println("messages.length---" + messages.length);
            DataRepository.updateText(System.getProperty("line.separator") + "Сообщений в ящике:" + messages.length + System.getProperty("line.separator"));

            GetMulti gmulti = new GetMulti();

            for (int i = 0, n = messages.length; i < n; i++) {

                builder.setContentTitle("Обработка сообщения: " + String.valueOf(i + 1) + " из " + String.valueOf(messages.length))
                        .setProgress(messages.length, i, false);
                notificationManager.notify(NOTIFY_ID, builder.build());

                Message message = messages[i];

                DataRepository.updateText(System.getProperty("line.separator") + "---------------------------------" + System.getProperty("line.separator"));
                DataRepository.updateText(System.getProperty("line.separator") + "Сообщение N " + (i + 1) + System.getProperty("line.separator"));
                String subject = message.getSubject().trim();
                DataRepository.updateText(System.getProperty("line.separator") + "Тема: " + subject + System.getProperty("line.separator"));

                Address[] froms = message.getFrom();
                String email = froms == null ? null : ((InternetAddress) froms[0]).getAddress();
                DataRepository.updateText(System.getProperty("line.separator") + "От кого: " + email + System.getProperty("line.separator"));

                String text = Html.fromHtml(gmulti.getText(message)).toString();
                DataRepository.updateText(System.getProperty("line.separator") + "Текст: " + text + System.getProperty("line.separator"));

                if (message_action.equals("Удалять")) {
                    message.setFlag(Flags.Flag.DELETED, true);
                } else {
                    message.setFlag(Flags.Flag.SEEN, true);
                }

                if (subject.equals(token)) {
                    text = text.trim(); //Удаляем пробелы вначале и конце строки
                    int space = text.indexOf(" ");
                    String phone = text.substring(0, space);

                    //Проеряем номер телефона на лишние символы (не цифры)
                    //при этом + в начале не трогаем
                    if (phone.charAt(0) == '+') {
                        phone = phone.replaceAll("[^0-9]", "");
                        phone = '+' + phone;
                    } else {
                        phone = phone.replaceAll("[^0-9]", "");
                    }
                    Log.i("MyTag", "Phone: " + phone);

                    String messageText = text.substring(space).trim();
                    messageText = messageText.replace("\n", "");
                    Log.i("MyTag", "Message: " + messageText);

                    // Текущее время
                    currentDate = new Date();
                    // Форматирование времени как "день.месяц.год"
                    dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    dateText = dateFormat.format(currentDate);
                    // Форматирование времени как "часы:минуты:секунды"
                    timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    timeText = timeFormat.format(currentDate);

                    contentValues.put(DBHelper.KEY_DATATIME, dateText + " " + timeText);
                    contentValues.put(DBHelper.KEY_TYPE, "Phone");
                    contentValues.put(DBHelper.KEY_MESSAGE, phone + " " + messageText + System.getProperty("line.separator"));

                    database.insert(DBHelper.TABLE_MESSAGE, null, contentValues);

                    DataRepository.updateText(System.getProperty("line.separator") + dateText + " " + timeText + " " + phone + " " + messageText + System.getProperty("line.separator"));

                    Log.i("MyTag", "DataTime: " + dateText + timeText);

                   // SmsManager.getDefault().sendTextMessage(phone, null, messageText, null, null);
                }
            }

            // close the store and folder objects
            //      emailFolder.close(false);
            emailFolder.close(true); //Удаляем сообщения из почты
            store.close();
            dbHelper.close();

        } catch (AuthenticationFailedException e) {
            DataRepository.updateText(System.getProperty("line.separator") + "Ошибка " +
                    "аутентификации. Проверьте имя и пароль." + System.getProperty("line.separator"));
            Log.i("MyTag", "Ошибка аутентификации. Проверьте имя и пароль.", e);
        } catch (MailConnectException e)   {
            if (isCausedBy(e, UnknownHostException.class)) {
                DataRepository.updateText(System.getProperty("line.separator") + "Ошибка " +
                        "в имени сервера. Проверьте имя сервера." + System.getProperty("line.separator"));
            }
        } catch (NoSuchProviderException e) {
            DataRepository.updateText(System.getProperty("line.separator") + e
                    + System.getProperty("line.separator"));
            Log.i("MyTag", "Ошибка в NoSuchProviderException.", e);
        } catch (MessagingException e) {
            DataRepository.updateText(System.getProperty("line.separator") +  e
                    + System.getProperty("line.separator"));
        } catch (Exception e) {
            DataRepository.updateText(System.getProperty("line.separator") +  e
                    + System.getProperty("line.separator"));
        }
        notificationManager.cancel(NOTIFY_ID);

    }

    /**
     * Recursive method to determine whether an Exception passed is, or has a cause, that is a
     * subclass or implementation of the Throwable provided.
     *
     * @param caught          The Throwable to check
     * @param isOfOrCausedBy  The Throwable Class to look for
     * @return  true if 'caught' is of type 'isOfOrCausedBy' or has a cause that this applies to.
     */
    private boolean isCausedBy(Throwable caught, Class<? extends Throwable> isOfOrCausedBy) {
        if (caught == null) return false;
        else if (isOfOrCausedBy.isAssignableFrom(caught.getClass())) return true;
        else return isCausedBy(caught.getCause(), isOfOrCausedBy);
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
