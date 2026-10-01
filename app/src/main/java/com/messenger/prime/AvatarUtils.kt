package com.messenger.prime

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.ObjectKey
import java.io.File

fun loadAvatarFileIntoView(context: Context, file: File, imageView: ImageView) {
    val isGif = file.name.lowercase().endsWith(".gif")
    val radiusPx = (14 * context.resources.displayMetrics.density).toInt()
    val signatureKey = ObjectKey(if (file.exists()) file.lastModified() else System.currentTimeMillis())
    if (isGif) {
        Glide.with(context)
            .asGif()
            .load(file)
            .centerCrop()
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    } else {
        Glide.with(context)
            .load(file)
            .transform(CenterCrop(), RoundedCorners(radiusPx))
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    }
}

fun loadAvatarUriIntoView(context: Context, uri: Uri, imageView: ImageView) {
    val uriStr = uri.toString().lowercase()
    val isGif = uriStr.endsWith(".gif") || uriStr.contains("gif")
    val radiusPx = (14 * context.resources.displayMetrics.density).toInt()
    val file = if ("file" == uri.scheme && uri.path != null) File(uri.path!!) else null
    val signatureKey = ObjectKey(if (file != null && file.exists()) file.lastModified() else System.currentTimeMillis())
    if (isGif) {
        Glide.with(context)
            .asGif()
            .load(uri)
            .centerCrop()
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    } else {
        Glide.with(context)
            .load(uri)
            .transform(CenterCrop(), RoundedCorners(radiusPx))
            .signature(signatureKey)
            .placeholder(R.drawable.ic_person)
            .into(imageView)
    }
}

fun getAvatarColor(name: String): Int {
    val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
    )
    val hash = name.hashCode()
    val index = (if (hash == Int.MIN_VALUE) 0 else Math.abs(hash)) % colors.size
    return colors[index].toColorInt()
}
