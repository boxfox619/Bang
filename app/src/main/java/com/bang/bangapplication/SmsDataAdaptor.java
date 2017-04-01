package com.bang.bangapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bang.bangapplication.sms.Person;

/**
 * Created by boxfox on 2017-04-02.
 */

public class SmsDataAdaptor {
    private LinearLayout layout;
    private LayoutInflater inflater;

    public SmsDataAdaptor(LinearLayout layout, LayoutInflater inflater) {
        this.layout = layout;
        this.inflater = inflater;
    }

    public void load(){
    }

    public Person createPerson(String name, String phone, String message) {
        Person person = new Person();
        person.setName(name);
        person.setNumber(phone);
        person.setMessage(message);
        return person;
    }

    public void addPerson(Person person) {
        View view = inflater.inflate(R.layout.message_item, null);
        ((TextView) view.findViewById(R.id.target)).setText(person.getName());
        ((TextView) view.findViewById(R.id.subInfo)).setText(person.getNumber());
        ((TextView) view.findViewById(R.id.content)).setText(person.getMessage());
        layout.addView(view, 0);
    }

}
