# SoFix
This library was designed for loading native library.

# Reolved issue
We may meet below errors when we use System.loadLibrary() to load native library(.so file).  
1. java.lang.UnsatisfiedLinkError: Couldn't load "soFileName": findLibrary returned null  
2. java.lang.UnsatisfiedLinkError: PathClassLoader[DexPathList[[zip file "data/app/[package].apk"],nativeLibraryDirectories=[/data/data/[package]/lib, /vendor/lib, /system/lib]]] couldn't find "*.so"  
3. java.lang.UnsatisfiedLinkError: dlopen failed: "data/app/[package]/lib/*.so" has bad ELF magic
4. java.lang.UnsatisfiedLinkError: dlopen failed: empty/missing DT_HASH in "data/app/[package]/lib/*.so"
5. java.lang.UnsatisfiedLinkError: dlopen failed: can't read file "data/app/[package]/lib/*.so": I/O error
6. java.lang.UnsatisfiedLinkError: dlopen failed: "data/app/[package]/lib/*.so" is too small to be an ELF executable  
# How to use
1. add library dependency to dependencies{} block in build.gradle file  
```
dependencies{
...
compile 'com.cantalou:androidSoFix:1.0.0'
}

```
2. add in java code  
