package com.example.email_to_sms2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.work.PeriodicWorkRequest;


public class SettingsActivity extends AppCompatActivity {

    private EditText email, password, smtp_server, port, time;
    View view;
    SharedPreferences sharePref;
    public PeriodicWorkRequest uploadWorkRequest;


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

     //Выводим стрелочку "Назад"
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharePref = PreferenceManager.getDefaultSharedPreferences(this);

    }

    //Обработка нажатия стрелки назад.
    //Так как это у нас единственный пункт меню, то нет необходимости его проверять
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
                this.finish();
                return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    //        setPreferencesFromResource(R.xml.root_preferences, rootKey);
            addPreferencesFromResource(R.xml.root_preferences);
        }
    }


}