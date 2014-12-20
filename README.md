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
2. Java SDK
3. Apache Tomcat (Ella collects coverage data from the instrumented app and shows the coverage report
through webapps)

## Setting up Ella
1. Rename `ella.settings.template` file to `ella.settings`.
2. Make following two changes in `ella.settings`. 
  1. Change the value of `android.jar` to the path to android.jar file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to `platforms/android-19/android.jar` inside the android SDK directory.
  2. Set the `jarsigner.*` variables to appropriate values. [jarsigner](http://docs.oracle.com/javase/6/docs/technotes/tools/windows/jarsigner.html) tool is used to sign instrumented apk's.
3. Before executing any ella-instrumented apps, push `ella_url.txt` to emulator/device's sdcard as follows. `ella_url.txt` file is generated when ella is built.
```
adb push ella_url.txt /sdcard/ella_url.txt
```

## Instrumenting the app
Execute the following command
```
ella.sh <path-to-apk>
```

This would produce the instrumented apk named `instrumented.apk` inside a subdirectory inside `<ella-home>/ella-out` directory, where `<ella-home>` represents the installation directory of ella. The name of the subdirectory is derived from `<path-to-apk>`.

## Collecting coverage
1. Install the `instrumented.apk` on the emulator or device using `adb` as follows. You may have to uninstall it first if the app is already installed.
2. Copy the `ella_record_coverage` file to emulator/device's sdcard as follows.
```
adb push ella_record_coverage /sdcard/ella_record_coverage
```
3. Execute the instrumented app. 
4. To stop recording coverage data and upload the data to ella webapp, delete the `/sdcard/ella_record_coverage' file as follows.
```
adb shell rm /sdcard/ella_record_coverage
```

## Viewing coverage report