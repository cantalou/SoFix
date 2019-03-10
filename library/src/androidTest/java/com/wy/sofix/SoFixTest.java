package com.wy.sofix;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wy.sofix.loader.SoLoadFailureException;
import com.wy.sofix.loader.SoLoader;
import com.wy.sofix.utils.ReflectUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Collection;


/**
 * @author cantalou
 * @date 2018-10-05 22:44
 */
@RunWith(AndroidJUnit4.class)
@Suite.SuiteClasses({SoFixTest.class})
public class SoFixTest {

    private static final String TAG = "SoFixTest";

    Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getTargetContext();

        ClassLoader classLoader = context.getClassLoader();
        Object pathList = ReflectUtil.getFieldValue(classLoader, "pathList");
        Object nativeLibraryDirectories = ReflectUtil.getFieldValue(pathList, "nativeLibraryDirectories");
        if (nativeLibraryDirectories instanceof Collection) {
            ((Collection) nativeLibraryDirectories).clear();
        } else {
            ReflectUtil.setFieldValue(pathList, "nativeLibraryDirectories", new File[]{});
        }

        Object nativeLibraryPathElements = ReflectUtil.getFieldValue(pathList, "nativeLibraryPathElements");
        if (nativeLibraryPathElements != null) {
            Class type = Array.get(nativeLibraryPathElements, 0)
                              .getClass();
            ReflectUtil.setFieldValue(pathList,"nativeLibraryPathElements" ,Array.newInstance(type, 0));
        }
    }

    @Test
    public void performLoad() {

        try {
            System.loadLibrary("test");
            Assert.fail();
        } catch (Throwable e) {
            e.printStackTrace();
            if (!(e instanceof UnsatisfiedLinkError)) {
                Assert.fail();
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