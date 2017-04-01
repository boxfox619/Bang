package com.bang.bangapplication.sms;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by boxfox on 2017-04-01.
 */

public class SMSManager {
    private List<Person> phoneList;
    private Context context;

    public SMSManager(Context context) {
        this.context = context;
    }

    public void sendSMS(String phone, String message) {
        SmsManager mSmsManager = SmsManager.getDefault();
        mSmsManager.sendTextMessage(phone, null, message, null, null);
    }

    private ArrayList<Person> getContactList() {
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
                String phonenumber = contactCursor.getString(1).replaceAll("-", "");
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

        return contactlist;
    }

    public Person getPhoneList(String name) {
        Person result = null;
        if(phoneList == null)
            phoneList = getContactList();
        for(Person person : phoneList){
            if(person.getName().equals(name))
                result = person;
        }
        return result;
    }
}
