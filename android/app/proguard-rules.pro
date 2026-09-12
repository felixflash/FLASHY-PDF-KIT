# Capacitor ProGuard Rules
-keep @com.getcapacitor.annotation.CapacitorPlugin public class * {
    @com.getcapacitor.annotation.PermissionCallback <methods>;
    @com.getcapacitor.annotation.ActivityCallback <methods>;
    @com.getcapacitor.annotation.Permission <methods>;
    @com.getcapacitor.PluginMethod public <methods>;
}
-keep public class * extends com.getcapacitor.Plugin { *; }

# Cordova Plugin Rules (if used)
-keep public class * extends org.apache.cordova.CordovaPlugin {
    public <methods>;
    public <fields>;
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
