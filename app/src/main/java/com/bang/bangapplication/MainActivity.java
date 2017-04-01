package com.bang.bangapplication;


import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bang.bangapplication.sms.Person;
import com.victor.loading.rotate.RotateLoading;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainActivity extends FIndDeviceActivity {
    private boolean isConnect;
    private float originalScale;
    private RotateLoading rotateLoading;
    private View circle;
    private ImageView mainImageView;

    private FloatingActionButton fab;
    private View nonDataView, mainView;
    private RelativeLayout mainContentView;

    private SmsDataAdaptor personAdaptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Realm.init(MainActivity.this);
        init();
        //new SMSManager(this).sendSMS("010-7350-7624", "~~");
    }

    private void init() {
        rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);
        mainImageView = (ImageView) findViewById(R.id.mainImage);
        circle = findViewById(R.id.normal_cirlce);
        originalScale = circle.getScaleX();

        mainView = getLayoutInflater().inflate(R.layout.main_data_view, null);
        nonDataView = findViewById(R.id.nonDataView);
        mainContentView = (RelativeLayout) findViewById(R.id.mainRootView);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConnect)
                    connecting();
                else {
                    showPersonManageDialog();
                }
            }
        });
    }

    private void setNonDataStatus(String text) {
        ((TextView) nonDataView.findViewById(R.id.statusTextView)).setText(text);
    }

    private void connecting() {
        rotateLoading.start();
        mainImageView.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth));
        mainContentView.removeAllViews();
        mainContentView.addView(nonDataView);
        fab.animate().translationY(500);
        setNonDataStatus("Connecting...");
        circle.animate().scaleX(0);
        circle.animate().scaleY(0);
        startService();
    }

    private void success() {
        isConnect = true;
        rotateLoading.stop();
        mainImageView.setImageDrawable(getResources().getDrawable(R.drawable.check));
        mainContentView.removeAllViews();
        mainContentView.addView(mainView);
        circle.animate().scaleX(originalScale);
        circle.animate().scaleY(originalScale);
        fab.animate().translationY(-30);
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add));
        personAdaptor = new SmsDataAdaptor(((LinearLayout) findViewById(R.id.personListLayout)), getLayoutInflater());
        personAdaptor.load();
    }

    @Override
    public void onConnected() {
        success();
    }

    @Override
    public void onDisconnected() {
        if (!isConnect)
            fab.animate().translationY(-30);
        isConnect = false;
        rotateLoading.stop();
        mainImageView.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth));
        mainContentView.removeAllViews();
        mainContentView.addView(nonDataView);
        circle.animate().scaleX(originalScale);
        circle.animate().scaleY(originalScale);
        setNonDataStatus("Need Connect");
    }

    private void showPersonManageDialog() {
        new PersonAddDialogBuilder(MainActivity.this, handler).build().show();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PersonAddDialogBuilder.ADD_PERSON:
                    personAdaptor.savePerson((Person) msg.obj);
                    break;
            }
        }
    };

}
