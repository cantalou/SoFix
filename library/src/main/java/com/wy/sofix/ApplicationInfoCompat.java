package com.wy.sofix;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.audiofx.AudioEffect;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;

import static com.wy.sofix.SoFix.TAG;

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
 * @date 2018-06-18 14:34
 */
public class ApplicationInfoCompat {

    private static PackageInfo getPackageInfoFromPM(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageInfo(context.getPackageName(), 0);
        } catch (Exception e) {
            Log.w(TAG, "getApplicationInfoFromPM: Failure while trying to obtain PackageInfo from PackageManager", e);
        }
        return null;
    }

    private static PackageInfo getPackageInfoFromCodePath(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getPackageArchiveInfo(context.getPackageCodePath(), 0);
        } catch (Exception e) {
            Log.w(TAG, "getApplicationInfoFromCodePath: Failure while trying to obtain PackageInfo from path " + context.getPackageCodePath(), e);
        }
        return null;
    }

    private static ApplicationInfo getApplicationInfoFromContext(Context context) {
        try {
            return context.getApplicationInfo();
        } catch (Exception e) {
            Log.w(TAG, "getApplicationInfoFromContext: Failure while trying to obtain ApplicationInfo from context " + context, e);
        }
        return null;
    }

    public static int getVersionCode(Context context) {
        PackageInfo packageInfo = getPackageInfo(context);
        return packageInfo != null ? packageInfo.versionCode : 0;
    }

    public static PackageInfo getPackageInfo(Context context) {
        PackageInfo packageInfo = getPackageInfoFromCodePath(context);
        if (packageInfo == null) {
            //package manager has died
            packageInfo = getPackageInfoFromPM(context);
        }
        return packageInfo;
    }

    public static ApplicationInfo getApplicationInfo(Context context) {
        ApplicationInfo applicationInfo = getApplicationInfoFromContext(context);
        if (applicationInfo == null) {
            PackageInfo packageInfo = getPackageInfo(context);
            if (packageInfo != null) {
                applicationInfo = packageInfo.applicationInfo;
            }
        }
        return applicationInfo;
    }

    /**
     * @param context
     * @return the path so file stored <br/>
     * rg:<br/>
     * 1. <= 4.0  /data/data/<package name>/lib
     * 2. >= 4.1  /data/app-lib/[package-name]-n
     */
    public static File getNativeLibraryDir(Context context) {
        ApplicationInfo info = getApplicationInfo(context);
        String nativeLibraryDir = info.nativeLibraryDir;
        if (TextUtils.isEmpty(nativeLibraryDir)) {
            nativeLibraryDir = info.dataDir + "/lib";
        }
        return new File(nativeLibraryDir);
    }
}
