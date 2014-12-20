ELLA: A Code Coverage Tool for Android APK's 
====

ELLA is a tool to instrument Android APK's to collect
coverage. Several tools exist that can instrument APK's to some
degree. But they usually do not work very reliably because they
translate Dalvik bytecodes to another form such as Java bytecode or
internal representations of other tools, and this translation is quite
challenging.  Thus, EMMA's approach is to instrument at the Dalvik
bytecode level. It does so by using the great DexLib2 library (a part
of the [Smali](https://github.com/JesusFreke/smali) project).

## Pre-requisite
1. Android SDK
2. Apache Tomcat (Ella collects coverage data from the instrumented app and shows the coverage report
through webapps)

## Setting up Ella
1. Copy ella.settings.template to ella.settings.
2. Change the value of android.jar in ella.settings to the path to android.jar file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to platforms/android-19/android.jar inside the android SDK directory.

## Instrumenting the app
Execute the following command
```
ella.sh <path-to-apk>
```

This would produce the instrumented apk named `instrumented.apk` inside a subdirectory inside `ella-out` directory.
The name of the subdirectory is derived from `<path-to-apk>`.

## Collecting coverage
Install the `instrumented.apk` on the emulator or device using `adb` as follows.
```
adb install instrumented.apk
```