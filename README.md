ELLA: A Code Coverage Tool for Android APK's 
====

ELLA is a tool to instrument Android APK's to collect
coverage. Currently, ella can measure method coverage.

Several tools exist that can instrument APK's to some
degree. But they usually do not work very reliably because they
translate Dalvik bytecodes to another form such as Java bytecode or
internal representations of other tools, and this translation is quite
challenging.  Thus, EMMA's approach is to instrument at the Dalvik
bytecode level. It does so by using the great DexLib2 library (a part
of the [Smali](https://github.com/JesusFreke/smali) project).

## Pre-requisite
1. Unix-like operating system. Minor tweaks to scripts and build files may be needed to run ella on Windows.
2. Android SDK
3. Java SDK
4. Ant
5. Apache Tomcat (Ella collects coverage data from the instrumented app and shows coverage report
through webapps). Download appropriate binary distribution from [Tomcat website](http://tomcat.apache.org/download-70.cgi).

## Before building ella
1. Rename `ella.settings.template` file to `ella.settings`.
2. Make following changes in `ella.settings`. 
  1. Set `android.jar` to the path to `android.jar` file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to `platforms/android-19/android.jar` inside the android SDK directory.
  2. Set `jarsigner.*` variables to appropriate values. [jarsigner](http://docs.oracle.com/javase/6/docs/technotes/tools/windows/jarsigner.html) tool is used to sign instrumented apk's.
  3. Set `tomcat.manager` to the username of a tomcat user who has `manager-script` role (i.e., can deploy webapps on the server). Tomcat username and passwords are listed in the file name `conf/tomcat-users.xml` inside Tomcat's installation directory.  For example, you must have a line similar to the following in your `conf/tomcat-users.xml`.

          <user username="ella-tomcat-manager" password="XXXX" roles="manager-script"/>

  4. Set `tomcat.password` to the above user's password. 
  5. Set `tomcat.dir` to installation directory of tomcat. 
  4. `tomcat.url` should be set to the ROOT URL of web server. Dont use `http://localhost:8080`; Instead, use the IP address. This URL is used by the instrumented app, which is *not* running locally, but on the instrumentor/phone.

## Build ella
Execute the following command inside ella's installation directory.
```
ant 
```

## Instrument the app
Execute the following command.
```
ella.i.sh <path-to-apk>
```

`<path-to-apk>` is the Apk that you want to instrument. This command would produce the instrumented apk named `instrumented.apk` inside a subdirectory inside `<ella-home>/ella-out` directory, where `<ella-home>` represents the installation directory of ella. The name of the subdirectory is derived from `<path-to-apk>`.

## Before executing ella-instrumented apps
Deploy ella webapp by issuing the following command in ella's installation directory.
```
ant -f frontend/build.xml deploy
```

## Collecting coverage
1. Install the `instrumented.apk` on the emulator or device. You may have to uninstall it first if the app is already installed.
2. Before executing the instrumented app, execute the following command on the PC connected to the device/emulator. `b` stands for "begin" as in begin recording coverage data.

        ella.r.sh b

3. Execute the instrumented app. 
4. To end recording coverage data and upload the data to ella webapp, execute the following command on the PC connected to the device/emulator. `e` stands for "end" in end recording coverage data.

        ella.r.sh e


## Viewing coverage report

The coverage data are stored inside a subdirectory of `<ella-home>/ella-out` directory, where `<ella-home>` represents the installation directory of ella. The name of the subdirectory is derived from `<path-to-apk>`. Currently, coverage data are stored in files `coverage.dat` and `covids`. `covids` contain the list of method signatures; index of a method is its identifier. `coverage.dat` contains the list of method identifiers that were executed.

There is a servlet that shows the set of covered method. At the end of the instrumentation step, ella prints out the URL to open to see the report.

## Troubleshooting

If the instrumented app appears to fail uploading the coverage data, check output of `adb logcat`. One likely cause is that the `tomcat.url` URL is not accessible from the emulator/phone.