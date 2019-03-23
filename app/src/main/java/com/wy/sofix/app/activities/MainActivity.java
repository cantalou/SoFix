package com.wy.sofix.app.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.wy.sofix.app.NativeLib;
import com.wy.sofix.app.R;
import com.wy.sofix.SoFix;
import com.wy.sofix.loader.SoLoadFailureException;

public class MainActivity extends Activity {

    TextView textContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textContent = findViewById(R.id.text_content);
    }

    public void loadLib(View view) {
        try {
            System.loadLibrary("native-lib");
            textContent.setText(NativeLib.getString());
        } catch (Throwable e) {
            textContent.setText(e.getMessage());
        }
    }

    public void repair(View view) {
        try {
            SoFix.loadLibrary(this, "native-lib");
        } catch (SoLoadFailureException e) {
            throw new RuntimeException(e);
        }
    }
}
