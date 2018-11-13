package com.example.opencv_grabcut;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Help extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setContentView(R.layout.activity_main);
    }
}

