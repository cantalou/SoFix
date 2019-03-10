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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.wy.sofix.BuildConfig;
import com.wy.sofix.utils.ReflectUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipFile;

import static com.wy.sofix.SoFix.TAG;

/**
 * @author cantalou
 * @date 2018-06-18 14:34
 */
public class ApplicationInfoCompat {

    private static File cachedNativeLibraryDir;

    private static SparseArray<String> cacheAbis = new SparseArray<>();

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
            packageInfo = getPackageInfoFromPM(context);
            //may null when throw exception(package manager has died)
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
     * @return
     */
    public static File getNativeLibraryDir(Context context) {
        if (cachedNativeLibraryDir != null) {
            return cachedNativeLibraryDir;
        }
        cachedNativeLibraryDir = getNativeLibraryDir(getApplicationInfo(context));
        return cachedNativeLibraryDir;
    }

    /**
     * get default nativeLibraryDir from ApplicationInfo
     *
     * @param info
     * @return path so file stored <br/>
     * rg:<br/>
     * 1. >= 4.0  /data/data/<[package-name]/lib         <br/>
     * 2. >= 4.1  /data/app-lib/[package-name]-n         <br/>
     * 3. >= 6.0  /data/app/[package-name]-n/lib/[arch]  <br/>
     * 4. >= 7.0  /data/app/[package-name]-n/lib/[arch]  <br/>
     */
    public static File getNativeLibraryDir(ApplicationInfo info) {
        String nativeLibraryDir = info.nativeLibraryDir;
        if (TextUtils.isEmpty(nativeLibraryDir)) {
            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt < Build.VERSION_CODES.JELLY_BEAN) {
                nativeLibraryDir = info.dataDir + "/lib";
            } else if (sdkInt < Build.VERSION_CODES.M) {
                nativeLibraryDir = "/data/app-lib/" + deriveCodePathName(info.sourceDir);
            } else {
                HashMap<String, String> instructionMap = getInstructionSetMap();
                try {
                    nativeLibraryDir = "/data/app/" + deriveCodePathName(info.sourceDir) + "/" + instructionMap.get(getPrimaryCpuAbi(info));
                } catch (Exception e) {
                    Log.w(TAG, "getNativeLibraryDir: ", e);
                }
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "getNativeLibraryDir: " + nativeLibraryDir);
            }
        }
        return TextUtils.isEmpty(nativeLibraryDir) ? null : new File(nativeLibraryDir);
    }

    public static String getPrimaryCpuAbi(Context context) {
        return getPrimaryCpuAbi(getApplicationInfo(context));
    }

    public static String getPrimaryCpuAbi(ApplicationInfo info) {
        try {
            return (String) ReflectUtil.getFieldValue(info, "primaryCpuAbi");
        } catch (Exception e) {
            Log.e(TAG, "getPrimaryCpuAbi: get 'primaryCpuAbi' from " + info + " error", e);
        }
        return "";
    }

    private static HashMap<String, String> getInstructionSetMap() {
        HashMap<String, String> map = new HashMap<>(16);
        map.put("armeabi", "arm");
        map.put("armeabi-v7a", "arm");
        map.put("mips", "mips");
        map.put("mips64", "mips64");
        map.put("x86", "x86");
        map.put("x86_64", "x86_64");
        map.put("arm64-v8a", "arm64");
        return map;
    }

    /**
     * Utility method that returns the relative package path with respect
     * to the installation directory. Like say for /data/data/com.test-1.apk
     * string com.test-1 is returned.
     */
    private static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        final File codeFile = new File(codePath);
        final String name = codeFile.getName();
        if (codeFile.isDirectory()) {
            return name;
        } else if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            final int lastDot = name.lastIndexOf('.');
            return name.substring(0, lastDot);
        } else {
            return null;
        }
    }

    /**
     * find the supported abi of this apk
     *
     * @param apkPath
     * @param apkZipFile
     * @param soFileName
     * @return
     */
    public static String getAbi(String apkPath, ZipFile apkZipFile, String soFileName) {
        int key = apkPath.hashCode();
        String cacheAbi = cacheAbis.get(key);
        if (cacheAbi != null) {
            return cacheAbi;
        }

        ArrayList<String> cpuAbi = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cpuAbi.addAll(Arrays.asList(Build.SUPPORTED_ABIS));
        } else {
            cpuAbi.add(Build.CPU_ABI);
            cpuAbi.add(Build.CPU_ABI2);
        }
        for (String abi : cpuAbi) {
            String entryPath = "lib" + File.separator + abi + File.separator + soFileName;
            if (apkZipFile.getEntry(entryPath) != null) {
                cacheAbis.put(key, abi);
                return abi;
            }
        }
        return "unknown";
    }
}
