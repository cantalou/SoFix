package com.wy.sofix.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.wy.sofix.NativeLib;
import com.wy.sofix.R;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textContent = findViewById(R.id.text_content);
        textContent.setText(NativeLib.getString());
    }
}
