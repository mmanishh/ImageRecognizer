package com.manishm.imagerecognizer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class InfoActivity extends AppCompatActivity {

    TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        tvVersion = (TextView) findViewById(R.id.tv_about_version);

        getSupportActionBar().hide();

        tvVersion.setText("v"+BuildConfig.VERSION_NAME);
    }
}
