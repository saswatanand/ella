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


# Setting up Ella
1. Copy ella.settings.template to ella.settings.
2. Change the value of android.jar in ella.settings to the path to android.jar file of the appropriate
android SDK version. For example, if you will execute the instrumented app in an emulator
with target API level 19, then use the path to platforms/android-19/android.jar inside the android SDK directory.

# Instrumenting the app
Execute the following command
```
ella.sh example.apk
```