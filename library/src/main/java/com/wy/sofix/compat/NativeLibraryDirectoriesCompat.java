/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.wy.sofix.compat;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import com.wy.sofix.BuildConfig;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.wy.sofix.SoFix.TAG;
import static com.wy.sofix.utils.ReflectUtil.getFieldValue;
import static com.wy.sofix.utils.ReflectUtil.invoke;
import static com.wy.sofix.utils.ReflectUtil.setFieldValue;

/**
 * @author cantalou
 * @date 2018-06-30 16:08
 */
public class NativeLibraryDirectoriesCompat {

    /**
     * Fix the field {@code nativeLibraryDirectories } in DexPathList if it does not contain path {@link ApplicationInfo#nativeLibraryDir}
     *
     * @param context
     */
    public static void fixNativeLibraryDirectories(Context context) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(context);
        if (nativeLibraryDir == null || !nativeLibraryDir.exists() || nativeLibraryDir.isFile()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "fixNativeLibraryDirectories: invalid dir " + nativeLibraryDir);
            }
            return;
        }
        appendNativeLibraryDir(context.getClassLoader(), nativeLibraryDir);
    }

    /**
     * Append new so file location to ClassLoader
     *
     * @param classLoader
     * @param nativeLibraryDir
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static void appendNativeLibraryDir(ClassLoader classLoader, File nativeLibraryDir) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "appendNativeLibraryDir: append nativeLibraryDir:" + nativeLibraryDir + " to " + classLoader);
        }

        if (nativeLibraryDir == null || !nativeLibraryDir.exists() || nativeLibraryDir.isFile()) {
            throw new IllegalArgumentException("file nativeLibraryDir:" + nativeLibraryDir + " must be directory and exist");
        }

        if (containsNativeLibraryDir(classLoader, nativeLibraryDir)) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "appendNativeLibraryDir: ignore " + nativeLibraryDir + "," + nativeLibraryDir + " contains this dir ");
            }
            return;
        }
        Object pathList = getPathList(classLoader);
        Object nativeLibraryDirectories = getNativeLibraryDirectories(pathList);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //add new element to nativeLibraryPathElements
            Object nativeLibraryPathElements = getNativeLibraryPathElements(pathList);
            Object[] newPathElement = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                newPathElement = (Object[]) invoke(pathList, "makePathElements", new Class[]{List.class}, Arrays.asList(nativeLibraryDir));
            } else {
                newPathElement = (Object[]) invoke(pathList, "makePathElements", new Class[]{List.class, File.class, List.class}, Arrays.asList(nativeLibraryDir), null, new ArrayList<>());
            }
            Object newInstance = expand(nativeLibraryPathElements, newPathElement[0]);
            setFieldValue(pathList, "nativeLibraryPathElements", newInstance);

            //add new element to nativeLibraryDirectories
            expand(nativeLibraryDirectories, nativeLibraryDir);
        } else {
            Object newInstance = expand(nativeLibraryDirectories, nativeLibraryDir);
            setFieldValue(pathList, "nativeLibraryDirectories", newInstance);
        }
        Log.i(TAG, "appendNativeLibraryDir: append success result:" + classLoader);

    }

    private static Object getNativeLibraryDirectories(Object instance) throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(instance, "nativeLibraryDirectories");
    }

    private static Object getNativeLibraryPathElements(Object instance) throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(instance, "nativeLibraryPathElements");
    }

    private static Object getPathList(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(classLoader, "pathList");
    }

    private static Object expand(Object original, Object... extraElements) {
        if (extraElements == null || extraElements.length == 0) {
            return original;
        }

        if (original == null) {
            return extraElements;
        }

        Class<?> originalClass = original.getClass();
        if (originalClass.isArray()) {
            int len = Array.getLength(original);
            Object combined = Array.newInstance(originalClass.getComponentType(), len + extraElements.length);
            System.arraycopy(extraElements, 0, combined, 0, extraElements.length);
            System.arraycopy(original, 0, combined, extraElements.length, len);
            return combined;
        } else if (original instanceof Collection) {
            for (Object obj : extraElements) {
                if (original instanceof List) {
                    ((List) original).add(0, obj);
                } else {
                    ((Collection) original).add(obj);
                }
            }
            return original;
        }
        return original;
    }

    private static boolean contains(Object original, File path) {
        Class<?> originalClass = original.getClass();
        if (originalClass.isArray()) {
            int len = Array.getLength(original);
            for (int i = len - 1; i >= 0; i--) {
                if (path.equals(Array.get(original, i))) {
                    return true;
                }
            }
        } else if (original instanceof Collection) {
            for (Object o : (Collection) original) {
                if (path.equals(o)) {
                    return true;
                }
            }
        }
        return original.equals(path);
    }

    public static boolean containsNativeLibraryDir(Context context) throws NoSuchFieldException, IllegalAccessException {
        return containsNativeLibraryDir(context, context.getClassLoader());
    }

    public static boolean containsNativeLibraryDir(Context context, ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(context);
        return containsNativeLibraryDir(classLoader, nativeLibraryDir);
    }

    public static boolean containsNativeLibraryDir(ClassLoader classLoader, File nativeLibraryDir) throws NoSuchFieldException, IllegalAccessException {
        Object pathList = getPathList(classLoader);
        Object nativeLibraryDirectories = getNativeLibraryDirectories(pathList);
        boolean result = contains(nativeLibraryDirectories, nativeLibraryDir);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "containsNativeLibraryDir: classLoader " + classLoader + " contains library path " + nativeLibraryDir + " " + result);
        }
        return result;
    }

}
