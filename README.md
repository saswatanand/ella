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
3. Ant
4. Apache Tomcat (Ella collects coverage data from the instrumented app and shows coverae report
through webapps)

## Before building ella
1. Rename `ella.settings.template` file to `ella.settings`, which must set various environment variables.
2. Make following three changes in `ella.settings`. 
  1. Change the value of `android.jar` to the path to `android.jar` file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to `platforms/android-19/android.jar` inside the android SDK directory.
  2. Set the `jarsigner.*` variables to appropriate values. [jarsigner](http://docs.oracle.com/javase/6/docs/technotes/tools/windows/jarsigner.html) tool is used to sign instrumented apk's.
  3. Set value of `tomcat.*` variables. `tomcat.manager` should be set to the username of a tomcat user who has `manager-script` role (i.e., can deploy webapps on the server). `tomcat.password` should be set to that user's password. `tomcat.dir` is the installation directory of tomcat. Tomcat username and passwords are listed in the file name `tomcat-users.xml` inside Tomcat's installation directory. `tomcat.url` should be set to the ROOT URL of web server.

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
```
ella.r.sh e
```

## Viewing coverage report
