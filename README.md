# SoFix
This library was designed for loading native library.

# Resolved issue
We may meet below errors when we use System.loadLibrary() to load native library(.so file).  
1. java.lang.UnsatisfiedLinkError: Couldn't load "soFileName": findLibrary returned null  
2. java.lang.UnsatisfiedLinkError: PathClassLoader[DexPathList[[zip file "data/app/[package].apk"],nativeLibraryDirectories=[/data/data/[package]/lib, /vendor/lib, /system/lib]]] couldn't find "*.so"  
3. java.lang.UnsatisfiedLinkError: dlopen failed: "data/app/[package]/lib/*.so" has bad ELF magic
4. java.lang.UnsatisfiedLinkError: dlopen failed: empty/missing DT_HASH in "data/app/[package]/lib/*.so"
5. java.lang.UnsatisfiedLinkError: dlopen failed: can't read file "data/app/[package]/lib/*.so": I/O error
6. java.lang.UnsatisfiedLinkError: dlopen failed: "data/app/[package]/lib/*.so" is too small to be an ELF executable  

# How to use
1. Add library dependency to dependencies{} block in build.gradle file  
```
dependencies{
...
compile 'com.cantalou:androidSoFix:1.0.0'
}

```
2. Add in java code  
```
SoFix.loadLibrary(context, "nativeLibraryName");
```  
if you want to load so with special classLoader
```
//This will load native library with classLoader of SoLoader.class, 
//default is classLoader of SoFix.class 
SoFix.loadLibrary(context, "test", new SoLoader() {
                @Override
                public void loadLibrary(String libName) {
                    System.loadLibrary(libName);
                }
                @Override
                public void load(String path) {
                    System.load(path);
                }
            });
```

# Test cover
1. Android version : from 4.1 - 9.0