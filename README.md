ELLA: A Code Coverage Tool for Android APK's 
====

Ella is a tool to instrument Android APK's to collect
coverage. Currently, ella can collect method coverage and time-stamped trace of covered methods.

Several tools exist that can instrument APK's to some
degree. But they usually do not work very reliably because they
translate Dalvik bytecodes to another form such as Java bytecode or
internal representations of other tools, and this translation is quite
challenging.  Thus, Ella's approach is to instrument at the Dalvik
bytecode level. It does so by using the great DexLib2 library (a part
of the [Smali](https://github.com/JesusFreke/smali) project).

## Pre-requisite
1. Unix-like operating system. Minor tweaks to scripts and build files may be needed to run ella on Windows.
2. Android SDK
3. Java SDK
4. Apache Ant

## Before building ella
1. Rename `ella.settings.template` file to `ella.settings`.
2. There are several environment variables. But only the following is mandatory, and need to be set in `ella.settings`. 
  1. Set `android.jar` to the path to `android.jar` file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to `platforms/android-19/android.jar` inside the android SDK directory.
## Build ella
Execute the following command inside ella's installation directory.
```
ant 
```

## Instrument the app
Execute the following command.
```
ella.sh i <path-to-apk>
```

`<path-to-apk>` is the Apk that you want to instrument. This command would produce the instrumented apk named `instrumented.apk` inside a subdirectory inside `<ella-home>/ella-out` directory, where `<ella-home>` represents the installation directory of ella. The name of the subdirectory is derived from `<path-to-apk>`.

## Before executing the instrumented app, start the ella server. 
```
ella.sh s
```

## Collecting coverage
1. Install the `instrumented.apk` on the emulator or device. You may have to uninstall it first if the app is already installed.
2. Execute the instrumented app. The instrumented app will send coverage data periodically to the ella server.
3. To end recording coverage data, execute the following command on computer connected to the device/emulator. `e` stands for "end" in end recording coverage data.
```
ella.sh e
```

## Coverage data

The coverage data are stored inside a subdirectory of `<ella-home>/ella-out` directory, where `<ella-home>` represents the installation directory of ella. The name of the subdirectory is derived from `<path-to-apk>`. Currently, coverage data are stored in files `coverage.dat` and `covids`. `covids` contain the list of method signatures; index of a method is its identifier. `coverage.dat` contains the list of method identifiers that were executed.

## Troubleshooting

If the instrumented app appears to fail uploading the coverage data, check output of `adb logcat`. One likely cause is that the `tomcat.url` URL is not accessible from the emulator/phone.