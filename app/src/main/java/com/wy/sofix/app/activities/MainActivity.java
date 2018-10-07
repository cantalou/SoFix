package com.wy.sofix.app.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.wy.sofix.app.NativeLib;
import com.wy.sofix.app.R;
import com.wy.sofix.SoFix;
import com.wy.sofix.loader.SoLoadFailureException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            SoFix.loadLibrary(this,"native-lib");
            TextView textContent = findViewById(R.id.text_content);
            textContent.setText(NativeLib.getString());
        } catch (SoLoadFailureException e) {
            e.printStackTrace();
        }
    }
}
