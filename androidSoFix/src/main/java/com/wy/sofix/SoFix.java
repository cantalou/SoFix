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
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.wy.sofix.compat.ApplicationInfoCompat;
import com.wy.sofix.loader.AsyncSoLoader;
import com.wy.sofix.loader.SoLoadFailureException;
import com.wy.sofix.loader.SoLoader;
import com.wy.sofix.utils.IoUtil;

import java.io.File;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import static com.wy.sofix.Extractor.extract;
import static com.wy.sofix.compat.NativeLibraryDirectoriesCompat.appendNativeLibraryDir;
import static com.wy.sofix.compat.NativeLibraryDirectoriesCompat.containsNativeLibraryDir;


/**
 * Extract and reload so file from apk file if System.loadLibrary() throws error like java.lang.UnsatisfiedLinkError: ... couldn't find "**".so
 *
 * @author cantalou
 * @date 2018-06-18 11:45
 */
public class SoFix {

    public static final String TAG = "SoFix";

    /**
     * Load fo file from "soLoader" and reload if need
     *
     * @param libName  the name of the library
     * @param soLoader load so file with original caller classLoader
     */
    public static void loadLibrary(Context context, String libName, SoLoader soLoader) throws SoLoadFailureException {

        if (context == null) {
            throw new NullPointerException("Param context was null");
        }

        if (TextUtils.isEmpty(libName)) {
            throw new IllegalArgumentException("Param libName can not be empty");
        }

        if (soLoader == null) {
            throw new NullPointerException("Param soLoader was null");
        }

        try {
            soLoader.loadLibrary(libName);
        } catch (UnsatisfiedLinkError error) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "loadLibrary: ", error);
            }
            performReload(context, libName, soLoader);
        }
    }


    public static void loadLibrary(final Context context, final String libName, final AsyncSoLoader soLoader) throws SoLoadFailureException {
        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... voids) {
                try {
                    loadLibrary(context, libName, soLoader);
                } catch (Throwable e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Throwable result) {
                if (result == null) {
                    soLoader.onSuccess();
                } else {
                    soLoader.onFailure(result);
                }
            }
        }.execute();
    }

    /**
     * @see SoFix#loadLibrary(Context, String, SoLoader)
     */
    public static void loadLibrary(Context context, String libName) throws SoLoadFailureException {
        loadLibrary(context, libName, new SoLoader() {
            @Override
            public void loadLibrary(String libName) {
                System.loadLibrary(libName);
            }

            @Override
            public void load(String path) {
                System.load(path);
            }
        });
    }

    /**
     * @param context
     * @param libName
     * @param soLoader
     * @throws SoLoadFailureException
     */
    public static void performReload(Context context, String libName, SoLoader soLoader) throws SoLoadFailureException {

        String soFileName = System.mapLibraryName(libName);

        ArrayList<String> soFileNames = new ArrayList<>();
        soFileNames.add(soFileName);

        ApplicationInfo applicationInfo = ApplicationInfoCompat.getApplicationInfo(context);

        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(applicationInfo);

        ClassLoader classLoader = soLoader.getClass()
                                          .getClassLoader();

        File apkFile = new File(context.getPackageCodePath());
        ZipFile apkZipFile = null;
        try {
            apkZipFile = IoUtil.getZipFileWithRetry(apkFile);
            String arch = ApplicationInfoCompat.getPrimaryCpuAbi(applicationInfo);
            if (TextUtils.isEmpty(arch)) {
                arch = ApplicationInfoCompat.getAbi(context.getPackageCodePath(), apkZipFile, soFileName);
            }
            performReload(apkZipFile, libName, classLoader, nativeLibraryDir, arch, soFileNames, soLoader);
        } catch (Throwable e) {
            throw new SoLoadFailureException(e);
        } finally {
            IoUtil.closeSilent(apkZipFile);
        }
    }

    public static void performReload(ZipFile apkZipFile, String library, ClassLoader cl, File nativeLibraryDir, String arch, ArrayList<String> soFileNames, SoLoader soLoader) throws IllegalAccessException, NoSuchFieldException, IOException, NoSuchMethodException, InvocationTargetException {
        if (!containsNativeLibraryDir(cl, nativeLibraryDir)) {
            appendNativeLibraryDir(cl, nativeLibraryDir);
        }
        try {
            reload(apkZipFile, library, nativeLibraryDir, arch, soFileNames, soLoader, false);
        } catch (UnsatisfiedLinkError e) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "reload: ", e);
            }
            reload(apkZipFile, library, nativeLibraryDir, arch, soFileNames, soLoader, true);
        }
    }

    private static void reload(ZipFile apkZipFile, String library, File nativeLibraryDir, String arch, ArrayList<String> soFileNames, SoLoader soLoader, boolean force) throws IOException {
        try {
            extract(apkZipFile, arch, nativeLibraryDir, soFileNames, force);
            soLoader.loadLibrary(library);
        } catch (UnsatisfiedLinkError e) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "reload: ", e);
            }
            String soFileName = System.mapLibraryName(library);
            String soPath = new File(nativeLibraryDir, soFileName).getAbsolutePath();
            soLoader.load(soPath);
        }
    }
}
