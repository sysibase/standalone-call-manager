# Proguard rules for Capacitor Call Manager Plugin

# Preserve Capacitor annotations and base classes
-keep @com.getcapacitor.annotation.CapacitorPlugin class *
-keep class com.getcapacitor.** { *; }

# Preserve plugin entry points and data classes
-keep class com.ibase.plugins.callmanager.** { *; }
-keepclassmembers class com.ibase.plugins.callmanager.** { *; }
-dontwarn com.ibase.plugins.callmanager.**
