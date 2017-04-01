package com.bang.bangapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bang.bangapplication.sms.Person;
import com.bang.bangapplication.sms.SMSManager;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by boxfox on 2017-04-02.
 */

public class PersonAddDialogBuilder {
    public static final int ADD_PERSON = 123;

    private Context context;
    private AlertDialog alertDialog;
    private LayoutInflater inflater;
    private Handler handler;

    public PersonAddDialogBuilder(Context context, Handler handler){
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        this.handler = handler;
    }

    public AlertDialog build() {
        AlertDialog.Builder builder;
        View layout = inflater.inflate(R.layout.persons_dialog, null);
        final SMSManager smsManager = new SMSManager(context);
        final SmsDataAdaptor adaptor = new SmsDataAdaptor((LinearLayout) layout.findViewById(R.id.phoneList), inflater, R.layout.person_item);
        final EditText searchText = ((EditText) layout.findViewById(R.id.ed_home_searchbar));
        builder = new AlertDialog.Builder(context);
        builder.setView(layout);
        alertDialog = builder.create();
        appendList(smsManager.getContactList(), adaptor);
        ((ImageView) layout.findViewById(R.id.iv_search)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = searchText.getText().toString();
                List<Person> list = smsManager.getContactList();
                if (!text.equals("")) {
                    list = smsManager.getPhoneList(text);
                }
                appendList(list, adaptor);
            }
        });
        return alertDialog;
    }

    private void appendList(List<Person> list, SmsDataAdaptor adaptor) {
        adaptor.clear();
        for (final Person person : list) {
            View view = adaptor.addPerson(person);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                   createMessageInputLayout(person);
                }
            });
        }
    }


    public void createMessageInputLayout(final Person person) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final View layout = inflater.inflate(R.layout.person_save_dialog, null);
        ((TextView) layout.findViewById(R.id.target)).setText(person.getName());
        ((TextView) layout.findViewById(R.id.subInfo)).setText(person.getNumber());
        ((ImageView) layout.findViewById(R.id.iv_erase)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) layout.findViewById(R.id.et_Message)).setText("");
            }
        });
        builder.setView(layout);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RealmConfiguration realmConfig = new RealmConfiguration
                        .Builder()
                        .deleteRealmIfMigrationNeeded()
                        .build();
                Realm.setDefaultConfiguration(realmConfig);
                Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                person.setMessage(((EditText) layout.findViewById(R.id.et_Message)).getText().toString());
                realm.commitTransaction();
                Message msg = Message.obtain();
                msg.what = ADD_PERSON;
                msg.obj = person;
                handler.sendMessage(msg);
            }
        });
        builder.setNegativeButton("취소", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        this.alertDialog = dialog;
    }
}
