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

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.wy.sofix.utils.IoUtil.IO_BUF_SIZE;
import static com.wy.sofix.utils.IoUtil.closeSilent;
import static com.wy.sofix.utils.IoUtil.copy;
import static com.wy.sofix.SoFix.TAG;

/**
 * @author cantalou
 * @date 2018-06-18 11:59
 */
public class Extractor {

    /**
     * The max times for so extracted error attempts
     */
    public static int MAX_RETRY_TIME = 3;

    /**
     * Extract file whose name in <code>soFileNames</code> from <code>apkZipFile</code> into dir <code>nativeLibraryDir</code>
     *
     * @param apkZipFile       apk file where the so extract from
     * @param arch             cpu type(eg:armeabi)
     * @param nativeLibraryDir
     * @param soFileNames
     * @param force            force retry extract so from apk fil
     * @return return new newNativeLibraryDir that so file installed if nativeLibraryDir could
     */
    public static void extract(ZipFile apkZipFile, String arch, File nativeLibraryDir, ArrayList<String> soFileNames, boolean force) throws IOException {

        if (apkZipFile == null) {
            throw new NullPointerException("param apkZipFile was null");
        }

        if (TextUtils.isEmpty(arch)) {
            throw new IllegalArgumentException("param arch can not be empty");
        }

        int times = MAX_RETRY_TIME;
        IOException ioException = null;
        while (times-- >= 0) {
            try {
                for (String soFileName : soFileNames) {
                    String entryPath = "lib" + File.separator + arch + File.separator + soFileName;
                    ZipEntry soEntry = apkZipFile.getEntry(entryPath);
                    File soFile = new File(nativeLibraryDir, soFileName);
                    extract(apkZipFile, soEntry, soFile, force);
                }
            } catch (IOException e) {
                Log.w(TAG, "extract: ", e);
                ioException = e;
            }
        }
        throw ioException;
    }

    private static void extract(ZipFile apkFile, ZipEntry soEntry, File soFile, boolean force) throws IOException {
        if (!force && soFile.exists() && soFile.canRead() && soFile.length() == soEntry.getSize() && getCrc(soFile) == soEntry.getCrc()) {
            return;
        }
        if (!soFile.delete()) {
            throw new IOException("can not delete file " + soFile);
        }
        copy(apkFile.getInputStream(soEntry), soFile);
        long fileCrc32 = getCrc(soFile);
        if (fileCrc32 != soEntry.getCrc()) {
            throw new IOException("the crc32 value '" + fileCrc32 + "' of '" + soFile + " does not match scr32 value '" + soEntry.getCrc() + "' of zip entry.");
        }
    }


    public static long getCrc(File file) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            CRC32 crc = new CRC32();
            byte[] bytes = new byte[IO_BUF_SIZE];
            int len;
            while ((len = inputStream.read(bytes)) != -1) {
                crc.update(bytes, 0, len);
            }
            return crc.getValue();
        } finally {
            closeSilent(inputStream);
        }
    }

}
