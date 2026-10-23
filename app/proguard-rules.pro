# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Coil
-keep class coil.** { *; }

# FastExcel
-keep class org.dhatim.fastexcel.** { *; }
-dontwarn org.dhatim.fastexcel.**

# Zip4j
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# Apache Commons Compress & XZ
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**

# Markdown
-keep class org.intellij.markdown.** { *; }
-dontwarn org.intellij.markdown.**

# Coroutines and WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Missing optional dependencies for PDFBox and XML Parsing
-dontwarn com.gemalto.jp2.**
-dontwarn javax.xml.stream.**
-dontwarn org.codehaus.stax2.**
-dontwarn com.fasterxml.aalto.**
