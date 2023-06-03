package com.termux.zerocore.activity;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;

public class UncaughtExceptionHandlerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uncaught_exception_handler);


        String error = getIntent().getStringExtra("error");


        EditText viewById = findViewById(R.id.error_text);

        viewById.setText(viewById.getText().toString() + "\nVersion:" + UUtils.getVersionName(UUtils.getContext()) + "\n\n" + error);

    }
}
