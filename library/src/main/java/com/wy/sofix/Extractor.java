package com.wy.sofix;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.wy.sofix.ApplicationInfoCompat.getVersionCode;

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
     * The max times of so extracted error attempts
     */
    public static int MAX_RETRY_TIME = 2;

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

        ZipFile apk = null;
        try {
            apk = new ZipFile(apkFile);
            for (String soFileName : soFileNames) {
                String entryPath = "lib" + File.separator + arch + File.separator + soFileName;
                ZipEntry zipEntry = apk.getEntry(entryPath);
                File installedSoFile = new File(nativeLibraryDir, soFileName);
                long soFileSize = zipEntry.getSize();

                LogUtil.log(StringUtils.fileToString(installedSoFile) + ", zipEntry.getSize " + soFileSize);
                if (!force && installedSoFile.exists() && installedSoFile.length() == soFileSize && installedSoFile.canRead()) {
                    continue;
                }

                File bakInstalledSoFile = new File(bakNativeLibraryDir, soFileName);
                LogUtil.log(StringUtils.fileToString(bakInstalledSoFile));
                if (!force && bakInstalledSoFile.exists() && bakInstalledSoFile.length() == soFileSize) {
                    continue;
                }

                bakInstalledSoFile.delete();
                int times = 0;
                while (times++ <) {
                    boolean result = copy(apk.getInputStream(zipEntry), bakInstalledSoFile);
                    if (result) {
                        break;
                    }
                }
                LogUtil.log("After copy times" + times + "," + StringUtils.fileToString(bakInstalledSoFile) + ", length " + bakInstalledSoFile.length());
            }
        } catch (IOException e) {
            LogUtil.log(e);
        } finally {
            try {
                apk.close();
            } catch (IOException e) {
                //ignore
            }
        }
        return bakNativeLibraryDir != nativeLibraryDir ? bakNativeLibraryDir : null;
    }
}
