package com.bang.bangapplication.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by boxfox on 2017-04-01.
 */

public class SMSManager {
    private List<Person> phoneList;
    private Context context;
    private LocationProvider provider;

    public SMSManager(Context context) {
        this.context = context;
        this.provider = LocationProvider.getInstance(context);
    }

    public void broadCast() {
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfig);
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Person> resultSet = realm.where(Person.class).findAll();
        final List<String> numbers = new ArrayList<String>();
        final List<String> messages = new ArrayList<String>();
        for (Person person : resultSet){
            numbers.add(person.getNumber());
            messages.add(person.getMessage());
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                String location = provider.getCurrentLocation();
                for (int i = 0 ; i < numbers.size();i++) {
                    sendSMS(numbers.get(i), messages.get(i), location);
                }
            }
        }).start();
    }

    public void sendSMS(String phone, String message, String... args) {
        for (String arg : args)
            message += "\n" + arg;
        SmsManager mSmsManager = SmsManager.getDefault();
        mSmsManager.sendTextMessage(phone, null, message, null, null);
    }

    public List<Person> getContactList() {
        if (phoneList != null) return phoneList;
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        String[] selectionArgs = null;
        String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        Cursor contactCursor = context.getContentResolver().query(uri, projection, null,
                selectionArgs, sortOrder);

        ArrayList<Person> contactlist = new ArrayList<Person>();

        if (contactCursor.moveToFirst()) {
            do {
                String phonenumber = contactCursor.getString(0).replaceAll("-", "");
                if (phonenumber.length() == 10) {
                    phonenumber = phonenumber.substring(0, 3) + "-"
                            + phonenumber.substring(3, 6) + "-"
                            + phonenumber.substring(6);
                } else if (phonenumber.length() > 8) {
                    phonenumber = phonenumber.substring(0, 3) + "-"
                            + phonenumber.substring(3, 7) + "-"
                            + phonenumber.substring(7);
                }

                Person acontact = new Person();
                acontact.setNumber(phonenumber);
                acontact.setName(contactCursor.getString(1));

                contactlist.add(acontact);
            } while (contactCursor.moveToNext());
        }

        phoneList = contactlist;
        return contactlist;
    }

    public List<Person> getPhoneList(String name) {
        List<Person> result = new ArrayList<Person>();
        if (phoneList == null)
            getContactList();
        for (Person person : phoneList) {
            if (person.getName().contains(name))
                result.add(person);
        }
        return result;
    }
}
