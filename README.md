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
1. Rename `ella.settings.template` file to `ella.settings`, and if needed, set values of different environment variables of ella.
2. If the instrumented app will be executed on an emulator **and** the ella server will be run on the host machine of the emulator, then do nothing. Otherwise, set:
```
ella.use.emulator.host.loopback=false
```
If `ella.use.emulator.host.loopback` is set to `false` **and** the ella server will be running on a machine that is different from the machine on which the instrumentor is run, then set `ella.server.ip` to the IP address of the machine on which the ella server will be run. For example,
```
ella.server.ip=1.2.3.4
```
Otherwise, do nothing.

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