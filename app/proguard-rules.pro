# قوانین مربوط به iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# قوانین مربوط به BouncyCastle برای امنیت و رمزنگاری
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# قوانین عمومی برای اندروید و متریال دیزاین
-keep class com.google.android.material.** { *; }
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.**

# جلوگیری از حذف متدهای مربوط به Glide (نمایش عکس)
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# جلوگیری از حذف کلاس‌های سیستمی
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn javax.annotation.**
-dontwarn java.awt.**
