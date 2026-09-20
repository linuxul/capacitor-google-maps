package com.hemangkumar.capacitorgooglemaps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.util.LruCache
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.getcapacitor.JSObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale

internal class AsyncIconLoader(jsIconDescriptor: JSObject, private val activity: FragmentActivity) {
    fun interface OnIconReady {
        fun onReady(bitmap: Bitmap?)
    }

    private val iconDescriptor = IconDescriptor(jsIconDescriptor)

    fun load(onIconReady: OnIconReady) {
        if (iconDescriptor.url.isEmpty()) {
            onIconReady.onReady(null)
            return
        }
        val url = iconDescriptor.url.lowercase(Locale.ROOT)
        val cachedBitmap = bitmapCache.get(url)
        if (cachedBitmap != null) {
            onIconReady.onReady(cachedBitmap)
            return
        }
        if (url.endsWith(".svg")) {
            loadSvg(onIconReady)
        } else {
            loadBitmap(onIconReady)
        }
    }

    private fun loadBitmap(onIconReady: OnIconReady) {
        val builder =
            Glide
                .with(activity)
                .asBitmap()
                .load(iconDescriptor.url)
                .timeout(PICTURE_DOWNLOAD_TIMEOUT)
        scaleImageOptional(builder).into(
            object : CustomTarget<Bitmap>() {
                // It will be called when the resource load has finished.
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmapCache.put(iconDescriptor.url, bitmap)
                    onIconReady.onReady(bitmap)
                }

                // It is called when a loadAll is cancelled and its resources are freed.
                override fun onLoadCleared(placeholder: Drawable?) {
                    // Use default marker
                    onIconReady.onReady(null)
                }

                // It is called when can't get image from network AND from a local cache.
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // Use default marker
                    onIconReady.onReady(null)
                }
            }
        )
    }

    private fun loadSvg(onIconReady: OnIconReady) {
        Glide.with(activity).downloadOnly().load(iconDescriptor.url).into(
            object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    try {
                        FileInputStream(resource).use { inputStream ->
                            val svg = SVG.getFromInputStream(inputStream)
                            svg.documentWidth = iconDescriptor.size.width.toFloat()
                            svg.documentHeight = iconDescriptor.size.height.toFloat()
                            val bitmap = pictureToBitmap(svg.renderToPicture())
                            bitmapCache.put(iconDescriptor.url, bitmap)
                            onIconReady.onReady(bitmap)
                        }
                    } catch (exception: IOException) {
                        onIconReady.onReady(null)
                    } catch (exception: SVGParseException) {
                        onIconReady.onReady(null)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    onIconReady.onReady(null)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    onIconReady.onReady(null)
                }
            }
        )
    }

    private fun <T> scaleImageOptional(builder: RequestBuilder<T>): RequestBuilder<T> =
        builder.override(iconDescriptor.size.width, iconDescriptor.size.height)

    private companion object {
        const val PICTURE_DOWNLOAD_TIMEOUT = 3000
        const val FAST_CACHE_SIZE_ENTRIES = 32

        val bitmapCache = LruCache<String, Bitmap>(FAST_CACHE_SIZE_ENTRIES)

        fun pictureToBitmap(picture: Picture): Bitmap {
            val pictureDrawable = PictureDrawable(picture)
            val bmp = Bitmap.createBitmap(pictureDrawable.intrinsicWidth, pictureDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawPicture(pictureDrawable.picture)
            return bmp
        }
    }
}
