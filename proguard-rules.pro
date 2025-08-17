# ProGuard configuration for Media Roulette Bot
# Basic obfuscation while keeping functionality

# Keep main class
-keep public class me.hash.mediaroulette.Main {
    public static void main(java.lang.String[]);
}

# Keep all classes that might be accessed via reflection
-keep class me.hash.mediaroulette.model.** { *; }
-keep class me.hash.mediaroulette.repository.** { *; }
-keep class me.hash.mediaroulette.bot.commands.** { *; }

# Keep JDA related classes
-keep class net.dv8tion.jda.** { *; }
-keep class club.minnced.** { *; }

# Keep MongoDB driver classes
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }

# Keep Jackson serialization classes
-keep class com.fasterxml.jackson.** { *; }
-keep @com.fasterxml.jackson.annotation.JsonIgnoreProperties class * { *; }
-keep @com.fasterxml.jackson.annotation.JsonProperty class * { *; }

# Keep Playwright classes
-keep class com.microsoft.playwright.** { *; }

# Keep logging classes
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Obfuscation settings
-repackageclasses 'obf'
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively

# Remove debug info
-printmapping mapping.txt
-printseeds seeds.txt
-printusage usage.txt

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# Don't warn about missing classes
-dontwarn **

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable