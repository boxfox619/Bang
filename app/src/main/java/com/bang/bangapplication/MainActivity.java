package com.bang.bangapplication;


import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.victor.loading.rotate.RotateLoading;

public class MainActivity extends ActionBarActivity {
    private boolean isConnecting;
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
        init();
        //new SMSManager(this).sendSMS("010-7350-7624", "~~");
    }

    private void init() {
        rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);
        mainImageView = (ImageView) findViewById(R.id.mainImage);
        circle = findViewById(R.id.normal_cirlce);
        originalScale = circle.getScaleX();

        nonDataView = getLayoutInflater().inflate(R.layout.non_data_view, null);
        mainView = findViewById(R.id.mainView);
        mainContentView = (RelativeLayout) findViewById(R.id.mainRootView);
        personAdaptor = new SmsDataAdaptor(((LinearLayout)findViewById(R.id.personListLayout)), getLayoutInflater());
        personAdaptor.addPerson(personAdaptor.createPerson("엄마", "010-5555-5531", "엄마 살려줘!"));

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnecting)
                    success();
                else
                    connecting();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_find) {
            Intent _i = null;
            _i = new Intent(MainActivity.this, FIndDeviceActivity.class);
            startActivity(_i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setNonDataStatus(String text){
        ((TextView)nonDataView.findViewById(R.id.statusTextView)).setText(text);
    }

    private void connecting() {
        isConnecting = true;
        rotateLoading.start();
        mainImageView.setImageDrawable(getResources().getDrawable(R.drawable.bluetooth));
        mainContentView.removeView(mainView);
        mainContentView.addView(nonDataView);
        setNonDataStatus("Connecting...");
        circle.animate().scaleX(0);
        circle.animate().scaleY(0);
    }

    private void success() {
        isConnecting = false;
        rotateLoading.stop();
        mainImageView.setImageDrawable(getResources().getDrawable(R.drawable.check));
        mainContentView.removeView(nonDataView);
        mainContentView.addView(mainView);
        circle.animate().scaleX(originalScale);
        circle.animate().scaleY(originalScale);
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_add));
    }
}
