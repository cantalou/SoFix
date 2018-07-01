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
package com.wy.sofix;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.SparseArray;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import dalvik.system.BaseDexClassLoader;

import static com.wy.sofix.ReflectUtil.getFieldValue;
import static com.wy.sofix.ReflectUtil.setFieldValue;

/**
 * @author cantalou
 * @date 2018-06-30 16:08
 */
public class NativeLibraryDirectoriesFix {

    private static SparseArray<Boolean> hasFixClassLoader = new SparseArray<>();

    /**
     * Fix the field {@code nativeLibraryDirectories } in DexPathList if it does not contain path {@link ApplicationInfo#nativeLibraryDir}
     *
     * @param context
     */
    public static void fixNativeLibraryDirectories(Context context) throws NoSuchFieldException, IllegalAccessException {
        appendNativeLibraryDir(context.getClassLoader(), ApplicationInfoCompat.getNativeLibraryDir(context));
    }

    /**
     * Append new so file location to ClassLoader
     *
     * @param classLoader
     * @param nativeLibraryDir
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static void appendNativeLibraryDir(ClassLoader classLoader, File nativeLibraryDir) throws NoSuchFieldException, IllegalAccessException {
        Object pathList = getPathList(classLoader);
        Object nativeLibraryDirectories = getNativeLibraryDirectories(pathList);
        if (contains(nativeLibraryDirectories, nativeLibraryDir)) {
            return;
        }
        Object newInstance = expand(nativeLibraryDirectories, nativeLibraryDir);
        if (newInstance != nativeLibraryDirectories) {
            updateNativeLibraryDir(pathList, newInstance);
        }
    }

    public static void updateNativeLibraryDir(Object instance, Object newNativeLibraryDirectories) throws NoSuchFieldException, IllegalAccessException {
        setFieldValue(instance, "nativeLibraryDirectories", newNativeLibraryDirectories);
    }

    public static Object getNativeLibraryDirectories(Object instance) throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(instance, "nativeLibraryDirectories");
    }

    private static Object getPathList(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(classLoader, "pathList");
    }

    private static Object expand(Object original, Object... extraElements) {
        if (extraElements == null || extraElements.length == 0) {
            return original;
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

    public static boolean containsNativeLibraryDir(Context context, ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        if (hasFixClassLoader.get(classLoader.hashCode())) {
            return true;
        }
        return containsNativeLibraryDir(classLoader, ApplicationInfoCompat.getNativeLibraryDir(context));
    }

    public static boolean containsNativeLibraryDir(ClassLoader classLoader, File nativeLibraryDir) throws NoSuchFieldException, IllegalAccessException {
        if (hasFixClassLoader.get(classLoader.hashCode())) {
            return true;
        }
        Object pathList = getPathList(classLoader);
        Object nativeLibraryDirectories = getNativeLibraryDirectories(pathList);
        boolean result = contains(nativeLibraryDirectories, nativeLibraryDir);
        hasFixClassLoader.put(classLoader.hashCode(), result);
        return result;
    }

}
