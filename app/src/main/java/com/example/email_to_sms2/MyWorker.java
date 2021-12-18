package com.example.email_to_sms2;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

public class MyWorker extends Worker {

    // Идентификатор уведомления
    private static final int NOTIFY_ID = 101;

    // Идентификатор канала
    private static String CHANNEL_ID = "123";

    DBHelper dbHelper;

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Email to SMS", NotificationManager.IMPORTANCE_LOW);
            NotificationManager mNotificationManager =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }


        String email = getInputData().getString("email");
        String password = getInputData().getString("password");
        String smtp_server = getInputData().getString("smtp_server");
        String port = getInputData().getString("port");

        Log.i("MyTag",email + " " + password + " " + smtp_server + " " + port);

        check(email, password, smtp_server, port);
        return null;
    }


    void check(String user, String password, String host, String port) {

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
            Log.i("MyTag", host+" " + user + " "+ password +" "+ port);
            store.connect(host, user, password);
            Log.i("MyTag", "connect");
            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            //    emailFolder.open(Folder.READ_ONLY);
            emailFolder.open(Folder.READ_WRITE);

            // retrieve the messages from the folder in an array and print it
            //   Message[] messages = emailFolder.getMessages();
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = emailFolder.search(ft);

            //   System.out.println("messages.length---" + messages.length);
            Log.i("MyTag", "Сообщений в ящике:" + messages.length);

            GetMulti gmulti = new GetMulti();

            for (int i = 0, n = messages.length; i < n; i++) {

                builder.setContentTitle("Обработка сообщения: " + String.valueOf(i+1) + " из " + String.valueOf(messages.length))
                        .setProgress(messages.length,i, false);
                notificationManager.notify(NOTIFY_ID, builder.build());

                Message message = messages[i];

                Log.i("MyTag", "---------------------------------");
                Log.i("MyTag", "Email Number " + (i + 1));
                String subject = message.getSubject().trim();
                Log.i("MyTag", "Subject: " + subject);

                Address[] froms = message.getFrom();
                String email = froms == null ? null : ((InternetAddress) froms[0]).getAddress();
                Log.i("MyTag", "From: " + email);

                String text = Html.fromHtml(gmulti.getText(message)).toString();
                Log.i("MyTag", "Text: " + text);

                // Помечаем сообщение на удаление
                message.setFlag(Flags.Flag.DELETED, true);

                if (subject.equals("1111")){
                    text = text.trim(); //Удаляем пробелы вначале и конце строки
                    int space = text.indexOf(" ");
                    String phone = text.substring(0,space);

                    //Проеряем номер телефона на лишние символы (не цифры)
                    //при этом + в начале не трогаем
                    if (phone.charAt(0) == '+') {
                        phone = phone.replaceAll("[^0-9]", "");
                        phone = '+' + phone;
                    } else {
                        phone = phone.replaceAll("[^0-9]", "");
                    }
                    Log.i("MyTag", "Phone: " + phone );

                    String messageText = text.substring(space).trim();
                    messageText = messageText.replace("\n", "");
                    Log.i("MyTag", "Message: " + messageText);

                    // Текущее время
                    Date currentDate = new Date();
                    // Форматирование времени как "день.месяц.год"
                    DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String dateText = dateFormat.format(currentDate);
                    // Форматирование времени как "часы:минуты:секунды"
                    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String timeText = timeFormat.format(currentDate);

                    contentValues.put(DBHelper.KEY_DATATIME,dateText + " " + timeText);
                    contentValues.put(DBHelper.KEY_TYPE, "Phone");
                    contentValues.put(DBHelper.KEY_MESSAGE, phone + " " + messageText +  System.getProperty("line.separator"));

                    database.insert(DBHelper.TABLE_MESSAGE, null, contentValues);

                    DataRepository.updateText(System.getProperty("line.separator") + dateText + " " + timeText + " " + phone + " " + messageText +  System.getProperty("line.separator"));

                    Log.i("MyTag", "DataTime: " + dateText + timeText);

                    // SmsManager.getDefault().sendTextMessage(phone, null, messageText, null, null);
                }
            }

            // close the store and folder objects
            //      emailFolder.close(false);
            emailFolder.close(true); //Удаляем сообщения из почты
            store.close();
            dbHelper.close();

        } catch (NoSuchProviderException e) {
            Log.i("MyTag", "Ошибка в NoSuchProviderException.", e);
        } catch (MessagingException e) {
            Log.i("MyTag", "Ошибка в MessagingException.", e);
        } catch (Exception e) {
            Log.i("MyTag", "Неизвестная ошибка!!!", e);
        }
        notificationManager.cancel(NOTIFY_ID);

    }
}
