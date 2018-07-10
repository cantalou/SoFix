package com.wy.sofix.compat;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.wy.sofix.utils.ReflectUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Random;

import dalvik.system.PathClassLoader;

import static org.junit.Assert.*;

/**
 * @author cantalou
 * @date 2018-07-08 18:55
 */
@LargeTest
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
        newNativeLibraryDir = new File(context.getFilesDir(), "newNativeLibraryDir" + new Random().nextInt(100));
        newNativeLibraryDir.mkdirs();
    }

    @Test
    public void fixNativeLibraryDirectories() throws Exception {
        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(context);
        assertTrue(nativeLibraryDir.exists() && !nativeLibraryDir.isFile());

        Object pathList = ReflectUtil.getFieldValue(classLoader, "pathList");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Field dexElementsField = ReflectUtil.findField(pathList, "nativeLibraryDirectories");
            dexElementsField.set(pathList, new File[]{});
        } else {
            ((Collection) ReflectUtil.getFieldValue(pathList, "nativeLibraryDirectories")).clear();
        }
        assertTrue(!classLoader.toString()
                               .contains(nativeLibraryDir.getName()));
        assertTrue(!NativeLibraryDirectoriesCompat.containsNativeLibraryDir(classLoader, nativeLibraryDir));

        NativeLibraryDirectoriesCompat.fixNativeLibraryDirectories(context);

        assertTrue(NativeLibraryDirectoriesCompat.containsNativeLibraryDir(classLoader, nativeLibraryDir));
        assertTrue(classLoader.toString()
                              .contains(nativeLibraryDir.getName()));
    }

    @Test
    public void appendNativeLibraryDir() throws Exception {
        assertTrue(!classLoader.toString()
                               .contains("newNativeLibraryDir"));
        assertTrue(!NativeLibraryDirectoriesCompat.containsNativeLibraryDir(classLoader, newNativeLibraryDir));

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