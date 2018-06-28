package com.wy.sofix;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
 * @author cantalou
 * @date 2018-06-18 11:59
 */
public class Extractor {

    private static final String TAG = "SoFix";

    /**
     * The max times for so extracted error attempts
     */
    public static int MAX_RETRY_TIME = 3;

    /**
     * @param apkFile          apk file path where the so extract from
     * @param arch             cpu type(eg:armeabi)
     * @param nativeLibraryDir
     * @param soFileNames
     * @param force            force retry extract so from apk fil
     * @return return new newNativeLibraryDir that so file installed if nativeLibraryDir could
     */
    public static void extract(Context context, File apkFile, String arch, File nativeLibraryDir, ArrayList<String> soFileNames, boolean force) throws IOException {

        if (context == null) {
            throw new NullPointerException("param context was null");
        }

        if (apkFile == null) {
            throw new NullPointerException("param apkFile was null");
        }

        if (!apkFile.exists()) {
            throw new FileNotFoundException("file " + apkFile + " does not exist");
        }

        if (!apkFile.canRead()) {
            throw new FileNotFoundException("param apiFile was null");
        }

        if (TextUtils.isEmpty(arch)) {
            throw new IllegalArgumentException("param arch can not be empty");
        }

        int times = MAX_RETRY_TIME;
        while (times-- >= 0) {
            try {
                extract(apkFile, arch, nativeLibraryDir, soFileNames, force);
            } catch (IOException e) {
                Log.w(TAG, "extract: ", e);
                if (times <= 0) {
                    throw e;
                }
            }
        }

    }

    private static void extract(File apkFile, String arch, File nativeLibraryDir, ArrayList<String> soFileNames, boolean force) throws IOException {
        ZipFile apk = null;
        try {
            apk = new ZipFile(apkFile);
            for (String soFileName : soFileNames) {
                String entryPath = "lib" + File.separator + arch + File.separator + soFileName;
                ZipEntry zipEntry = apk.getEntry(entryPath);
                File bakInstalledSoFile = new File(nativeLibraryDir, soFileName);
                if (!force && bakInstalledSoFile.exists() && bakInstalledSoFile.canRead() && bakInstalledSoFile.length() == zipEntry.getSize()) {
                    continue;
                }
                bakInstalledSoFile.delete();
                copy(apk.getInputStream(zipEntry), bakInstalledSoFile);
            }
        } finally {
            try {
                apk.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }

    private static void copy(InputStream is, File out) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(out);
        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 1024 * 16);
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            byte[] buf = new byte[1024 * 16];
            int len;
            while ((len = bis.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            bos.flush();
            fileOutputStream.getFD()
                            .sync();
        } finally {
            closeSilent(bis, bos);
        }
    }

    private static void closeSilent(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

}
