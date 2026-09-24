# Pantheon Launcher — ProGuard rules (release builds only; debug is unshrunk).
#
# v6: the old blanket `-keep com.apexforge.godlauncher.**` defeated R8
# shrinking (the 58MB debug APK problem). Keep only what reflection needs:
# DataStore/protobuf internals and serializable model classes referenced
# by name. Everything else may be shrunk/obfuscated.
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.Serializer { *; }
-dontwarn java.lang.invoke.**
