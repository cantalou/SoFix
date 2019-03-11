package com.wy.sofix.app;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.wy.sofix.compat.ApplicationInfoCompat;
import com.wy.sofix.utils.IoUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
@Suite.SuiteClasses({SoFixApplicationTest.class})
public class SoFixApplicationTest {

    @Test
    public void soFileMissingTest() throws Exception {

        File nativeLibDir = ApplicationInfoCompat.getNativeLibraryDir(SoFixApplication.globalContext);
        assertTrue(nativeLibDir.exists());

        File soFile = new File(nativeLibDir, "libtest.so");
        assertTrue(soFile.exists());

        String cmd = "su root rm -rf " + soFile.getAbsolutePath();

        Log.w("SoFixApplicationTest", cmd);

        Process process = Runtime.getRuntime().exec(cmd);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line = br.readLine();
            if (line != null) {
                Log.w("SoFixApplicationTest", line);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = br.readLine();
            if (line != null) {
                Log.w("SoFixApplicationTest", line);
            }
        }


        assertTrue(!soFile.exists());
    }
}