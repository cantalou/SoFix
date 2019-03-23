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
 */
package com.wy.sofix.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * @author cantalou
 * @date 2018-07-01 11:43
 */
public class IoUtil {

    public static final int IO_BUF_SIZE = 1024 * 4;

    /**
     * global retry time when error thrown
     */
    public static final int RETRY_TIMES = 3;

    /**
     * Copy data from a source stream to destFile.
     * Return true if succeed, return false if failed.
     */
    public static boolean copy(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[IO_BUF_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static void closeSilent(Closeable... closeables) {
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

    /***
     * compat version thar below API
     * @param closeables
     */
    public static void closeSilent(ZipFile... closeables) {
        for (ZipFile closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static ZipFile getZipFileWithRetry(File file) throws IOException {
        IOException exception = null;
        int times = 0;
        while (times++ < RETRY_TIMES) {
            try {
                return new ZipFile(file);
            } catch (IOException e) {
                exception = e;
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e1) {
                    //ignore
                }
            }
        }
        throw exception;
    }
}
