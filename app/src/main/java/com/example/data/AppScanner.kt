package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppModel(
    val packageName: String,
    val appName: String,
    val icon: Bitmap? = null
)

class AppScanner(private val context: Context) {

    fun scanLaunchableApps(): List<AppModel> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        // Query activities that can be launched by launcher icons
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        
        return resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            
            // Exclude our own app from the list
            if (packageName == context.packageName) return@mapNotNull null
            
            val iconDrawable = try {
                resolveInfo.loadIcon(pm)
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }
            
            val bmp = drawableToBitmap(iconDrawable)
            AppModel(
                packageName = packageName,
                appName = label,
                icon = bmp
            )
        }.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
