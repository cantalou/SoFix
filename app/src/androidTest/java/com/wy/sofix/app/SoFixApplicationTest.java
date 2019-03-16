package com.wy.sofix.app;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import com.chrisplus.rootmanager.RootManager;
import com.chrisplus.rootmanager.container.Result;
import com.chrisplus.rootmanager.container.Shell;
import com.wy.sofix.SoFix;
import com.wy.sofix.compat.ApplicationInfoCompat;
import com.wy.sofix.loader.SoLoadFailureException;
import com.wy.sofix.utils.ReflectUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Collection;

import static org.junit.Assert.*;


@RunWith(AndroidJUnit4.class)
@Suite.SuiteClasses({SoFixApplicationTest.class})
public class SoFixApplicationTest {

    Context context;

    Context textContext;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        textContext = InstrumentationRegistry.getContext();
    }


    @Test
    public void soFileMissingTest() throws Exception {
        File nativeLibDir = ApplicationInfoCompat.getNativeLibraryDir(SoFixApplication.globalContext);
        assertTrue(nativeLibDir.exists());

        File soFile = new File(nativeLibDir, "libtest.so");
        assertTrue(soFile.exists());

        RootManager manager = RootManager.getInstance();
        assertTrue(manager.hasRooted());
        manager.obtainPermission();

        //waiting for permission
        ReflectUtil.setFieldValue(Shell.class, "shellTimeout", 1000 * 1000);

        String cmd = " rm -f " + soFile.getAbsolutePath();
        Log.w("SoFixApplicationTest", cmd);
        Result result = manager.runCommand(cmd);
        Log.w("SoFixApplicationTest", result.getMessage());

        //compat some version
        cmd = " rm " + soFile.getAbsolutePath();
        result = manager.runCommand(cmd);
        Log.w("SoFixApplicationTest", result.getMessage());

        Process process = Runtime.getRuntime().exec("su " + cmd);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Log.w("SoFixApplicationTest", "runtime exec :" +  reader.readLine());

        assertTrue(!soFile.exists());

        try {
            System.loadLibrary("test");
            fail("so can not be loaded here");
        } catch (Throwable e) {
            if (!(e instanceof UnsatisfiedLinkError)) {
                org.junit.Assert.fail();
            }
        }

        SoFix.loadLibrary(SoFixApplication.globalContext, "test");

    }

    @Test
    public void emptyLibraryDirectoryTest() throws Exception {

        ClassLoader classLoader = context.getClassLoader();
        Object pathList = ReflectUtil.getFieldValue(classLoader, "pathList");
        Object nativeLibraryDirectories = ReflectUtil.getFieldValue(pathList, "nativeLibraryDirectories");
        if (nativeLibraryDirectories instanceof Collection) {
            ((Collection) nativeLibraryDirectories).clear();
        } else {
            ReflectUtil.setFieldValue(pathList, "nativeLibraryDirectories", new File[]{});
        }

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            Object nativeLibraryPathElements = ReflectUtil.getFieldValue(pathList, "nativeLibraryPathElements");
            if (nativeLibraryPathElements != null) {
                Class type = Array.get(nativeLibraryPathElements, 0)
                        .getClass();
                ReflectUtil.setFieldValue(pathList, "nativeLibraryPathElements", Array.newInstance(type, 0));
            }
        }

        try {
            System.loadLibrary("test");
            org.junit.Assert.fail();
        } catch (Throwable e) {
            if (!(e instanceof UnsatisfiedLinkError)) {
                org.junit.Assert.fail();
            }
        }

        try {
            SoFix.loadLibrary(context, "test");
        } catch (SoLoadFailureException e) {
            if (e != null) {
                e.printStackTrace();
                Assert.fail();
            }

        }
    }

}