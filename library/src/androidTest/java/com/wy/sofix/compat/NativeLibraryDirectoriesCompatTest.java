package com.wy.sofix.compat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;

import dalvik.system.PathClassLoader;

import static org.junit.Assert.*;

/**
 * @author cantalou
 * @date 2018-07-08 18:55
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@Suite.SuiteClasses({NativeLibraryDirectoriesCompatTest.class})
public class NativeLibraryDirectoriesCompatTest {

    Context context;

    PathClassLoader classLoader;

    File newNativeLibraryDir;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();
        classLoader = (PathClassLoader) context.getClassLoader();
        newNativeLibraryDir = new File(context.getFilesDir(), "newNativeLibraryDir");
        newNativeLibraryDir.mkdirs();
    }

    @Test
    public void fixNativeLibraryDirectories() throws Exception {
        NativeLibraryDirectoriesCompat.fixNativeLibraryDirectories(context);
    }

    @Test
    public void appendNativeLibraryDir() throws Exception {
        NativeLibraryDirectoriesCompat.appendNativeLibraryDir(classLoader, newNativeLibraryDir);
        assertTrue(classLoader.toString()
                              .contains("newNativeLibraryDir"));
        assertTrue(NativeLibraryDirectoriesCompat.containsNativeLibraryDir(classLoader, newNativeLibraryDir));
    }

    @Test
    public void containsNativeLibraryDir() throws Exception {
        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(context);
        boolean result = NativeLibraryDirectoriesCompat.containsNativeLibraryDir(classLoader, nativeLibraryDir);
        assertTrue(result);
    }
}