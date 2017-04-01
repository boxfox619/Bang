package com.bang.bangapplication;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bang.bangapplication.sms.Person;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by boxfox on 2017-04-02.
 */

public class SmsDataAdaptor extends Handler {
    private LinearLayout layout;
    private LayoutInflater inflater;
    private int layoutId;

    public SmsDataAdaptor(LinearLayout layout, LayoutInflater inflater) {
        this.layout = layout;
        this.inflater = inflater;
        this.layoutId = R.layout.message_item;
    }

    public SmsDataAdaptor(LinearLayout layout, LayoutInflater inflater, int layoutId) {
        this.layout = layout;
        this.inflater = inflater;
        this.layoutId = layoutId;
    }

    public void load() {
        clear();
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfig);
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Person> resultSet = realm.where(Person.class).findAll();
        for (Person person : resultSet) {
            addPerson(person);
        }
    }

    public void clear(){
        layout.removeAllViews();
    }

    public void savePerson(Person person) {
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfig);
        Realm realm = Realm.getDefaultInstance();
        if(realm.where(Person.class).equalTo("name", person.getName()).findFirst()!=null){
            load();
        }else {
            realm.beginTransaction();
            realm.copyToRealm(person);
            realm.commitTransaction();
            addPerson(person);
        }
    }

    public Person createPerson(String name, String phone, String message) {
        Person person = new Person();
        person.setName(name);
        person.setNumber(phone);
        person.setMessage(message);
        return person;
    }

    public View addPerson(final Person person) {
        View view = inflater.inflate(layoutId, null);
        ((TextView) view.findViewById(R.id.target)).setText(person.getName());
        ((TextView) view.findViewById(R.id.subInfo)).setText(person.getNumber());
        if (view.findViewById(R.id.content) != null){
            ((TextView) view.findViewById(R.id.content)).setText(person.getMessage());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new PersonAddDialogBuilder(v.getContext(), SmsDataAdaptor.this).createMessageInputLayout(person);
                }
            });
        }
        layout.addView(view, 0);
        return view;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case PersonAddDialogBuilder.ADD_PERSON:
                savePerson((Person) msg.obj);
                break;
        }
    }

}
