package com.wy.sofix;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.BaseDexClassLoader;

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
 * Extract and reload so file from apk file if System.loadLibrary() throws error like java.lang.UnsatisfiedLinkError: ... couldn't find "**".so
 *
 * @author cantalou
 * @date 2018-06-18 11:45
 */
public class SoFix {

    public static final String TAG = "SoFix";

    /**
     * Fix the field {@code nativeLibraryDirectories } in DexPathList if it does not contain path "nativeLibraryDir"
     *
     * @param classLoader
     */
    public static void fixNativeLibraryDirectories(BaseDexClassLoader classLoader) {
    }

    /**
     * Load fo file from "soLoader" and reload if need
     *
     * @param libName  the name of the library
     * @param soLoader load so file with original caller classLoader
     */
    public static void loadLibrary(Context context, String libName, SoLoader soLoader) {

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
            reload(context, libName, soLoader);
        }
    }

    /**
     * @see SoFix#loadLibrary(Context, String, SoLoader)
     */
    public static void loadLibrary(Context context, String libName) {
        loadLibrary(context, libName, new SoLoader() {
            @Override
            public void loadLibrary(String libName) {
                System.loadLibrary(libName);
            }
        });
    }


    /**
     * @param libName
     * @param soLoader
     */
    public static void reload(Context context, String libName, SoLoader soLoader) {

        String soFileName = System.mapLibraryName(libName);

        ArrayList<String> soFileNames = new ArrayList<>();
        soFileNames.add(soFileName);

        ApplicationInfo applicationInfo = ApplicationInfoCompat.getApplicationInfo(context);

        File nativeLibraryDir = ApplicationInfoCompat.getNativeLibraryDir(applicationInfo);

        ClassLoader loader = soLoader.getClass()
                                     .getClassLoader();

        File apkFile = new File(pluginPackage.getPluginPath());

        //现在发布包中只包含armeabi类型的so, 这里不做类型检测
        try {
            reload(app, apkFile, library, soFileNames, cl, nativeLibraryDir, "armeabi", false, soLoader);
        } catch (UnsatisfiedLinkError e) {
            LogUtil.log(e);
            reload(app, apkFile, library, soFileNames, cl, nativeLibraryDir, "armeabi", true, soLoader);
        } finally {
            StatisticsAgent.reportError(app, LogUtil.getLog());
        }
    }

    /**
     * so文件加载失败重试</>
     * 1.检测so文件的状态, 不存在文件时解压so文件到nativeLibraryDir目录
     * 2.解压后再次加载so文件, 如果还是失败, 则直接从释放目录里面读取so文件
     *
     * @param context
     * @param library
     * @param soFileNames
     * @param cl
     * @param nativeLibraryDir
     * @param apkFile
     * @param arch
     */
    public static void reload(Context context, File apkFile, String library, ArrayList<String> soFileNames, ClassLoader cl, File nativeLibraryDir, String arch, boolean force,
                              SoLoader.SoLoader soLoader) {
        File newNativeLibraryDir = checkSoInstall(context, apkFile, nativeLibraryDir, cl, arch, soFileNames, force);
        try {
            LogUtil.log("use System.loadLibrary to load " + library);
            soLoader.loadLibrary(library);
            LogUtil.log("use System.loadLibrary to load " + library + " success");
        } catch (UnsatisfiedLinkError error) {
            LogUtil.log("After unzip retry loadLibrary error " + Log.getStackTraceString(error));
            String soFileName = System.mapLibraryName(library);
            String soPath = new File(newNativeLibraryDir != null ? newNativeLibraryDir : nativeLibraryDir, soFileName).getAbsolutePath();
            LogUtil.log("try to load direct " + StringUtils.fileToString(soPath));
            soLoader.load(soPath);
        }
    }


    public static File checkSoInstall(Context context, File apkFile, File nativeLibraryDir, ClassLoader classLoader, String arch, ArrayList<String> soFileNames, boolean force) {
        LogUtil.log("checkSoInstall context:" + context + ", apkFile:" + StringUtils.fileToString(apkFile) + ", nativeLibraryDir:" + StringUtils.fileToString(
                nativeLibraryDir) + ", classLoader:" + classLoader + ", arch:" + arch + ", soFileNames:" + soFileNames + ", force:" + force);
        File newNativeLibraryDir = extract(context, apkFile, arch, nativeLibraryDir, soFileNames, force);
        if (newNativeLibraryDir != null) {
            Object dexPathList = getPathList(classLoader);
            Object nativeLibraryDirectories = getNativeLibraryDirectories(dexPathList);
            LogUtil.log("Fix so install error, dexPathList " + dexPathList + ", nativeLibraryDirectories " + nativeLibraryDirectories);
            LogUtil.log((Object) dexPathList.getClass()
                                            .getDeclaredFields());
            if (nativeLibraryDirectories instanceof File[]) {
                updateNativeLibraryDir(dexPathList, expand(nativeLibraryDirectories, newNativeLibraryDir));
            }
            LogUtil.log("After fix " + classLoader);
        }
        return newNativeLibraryDir;
    }

    private static Object expand(Object original, Object... extraElements) {
        Class<?> originalClass = original.getClass();
        if (originalClass.isArray()) {
            int len = Array.getLength(original);
            Object[] combined = ((Object[]) Array.newInstance(originalClass.getComponentType(), len + extraElements.length));
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


    private static boolean copy(InputStream is, File out) throws IOException {
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
        } catch (Throwable e) {
            LogUtil.log(e);
            return false;
        } finally {
            close(bis, bos);
        }
        return true;
    }

    private static void close(Closeable... closeables) {
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


    /**
     * 1. 检测ClassLoader中nativeLibraryDir数组中所包含的so库的路径信息
     * 2. 检测nativeLibraryDir目录是否包含对应APK内CPU版本的所有so文件(待实现)
     *
     * @param app
     */
    public static void check(Application app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return;
        }

        if (!AppUtils.checkCurrentProcess(app.getPackageName())) {
            return;
        }

        try {
            checkSoPath(getNativeLibraryDir(app));
        } catch (Throwable e) {
            LogUtil.log(e);
            StatisticsAgent.reportError(app, LogUtil.getLog());
        }
    }

    /**
     * 检测ClassLoader中是否包含App的so路径, 在某些机器上只包含[/vendor/lib, /system/lib]
     * 不包含/data/app-lib/package-2, /data/data/package/lib目录
     *
     * @param nativeLibraryDir so文件安装目录
     */
    public static void checkSoPath(File nativeLibraryDir) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            return;
        }
        LogUtil.log("nativeLibraryDir " + nativeLibraryDir);

        ClassLoader cl = PluginModelManager.class.getClassLoader();
        LogUtil.log("ClassLoader " + cl);

        if (cl instanceof BaseDexClassLoader) {
            Object dexPathList = getPathList(cl);
            File[] nativeLibraryDirectories = (File[]) getNativeLibraryDirectories(dexPathList);
            if (nativeLibraryDirectories == null) {
                nativeLibraryDirectories = new File[0];
            }
            for (File dir : nativeLibraryDirectories) {
                if (dir.equals(nativeLibraryDir)) {
                    return;
                }
            }
            appendNativeLibraryDir(nativeLibraryDir, dexPathList, nativeLibraryDirectories);
            LogUtil.log("After fix " + cl);
            StatisticsAgent.reportError(PluginManager.getInstance()
                                                     .getApplication(), LogUtil.getLog());
        }
    }

    private static void appendNativeLibraryDir(File nativeLibraryDir, Object dexPathList, Object[] nativeLibraryDirectories) {
        updateNativeLibraryDir(dexPathList, expand(nativeLibraryDirectories, nativeLibraryDir));
    }

    private static void updateNativeLibraryDir(Object instance, Object newNativeLibraryDirectories) {
        RefInvoker.setField(instance, instance.getClass(), "nativeLibraryDirectories", newNativeLibraryDirectories);
    }

    private static Object getNativeLibraryDirectories(Object instance) {
        return RefInvoker.getField(instance, instance.getClass(), "nativeLibraryDirectories");
    }

    private static Object getPathList(ClassLoader cl) {
        return RefInvoker.getField(cl, BaseDexClassLoader.class, "pathList");
    }


}
