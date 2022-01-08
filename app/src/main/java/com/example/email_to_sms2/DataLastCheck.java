package com.example.email_to_sms2;

import androidx.lifecycle.MutableLiveData;

public class DataLastCheck {
    private static MutableLiveData<String> data;
    /**
     * @return the {@link MutableLiveData} object.
     */
    public static MutableLiveData<String> getData() {
        if (data == null) {
            data = new MutableLiveData<String>();
        }
        return data;
    }

    public static void updateText(String t) {
        data.postValue(t);
    }
}
