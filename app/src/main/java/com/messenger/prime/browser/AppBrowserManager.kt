package com.messenger.prime.browser

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentActivity

object AppBrowserManager {

    @JvmStatic
    fun openUrl(context: Context, rawUrl: String?) {
        if (rawUrl.isNullOrEmpty()) return

        var targetUrl = rawUrl.trim()
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://") &&
            !targetUrl.startsWith("file://") && !targetUrl.startsWith("about:")
        ) {
            targetUrl = "https://$targetUrl"
        }

        Log.d("BrowserDebug", "Opening browser for url: $targetUrl")

        val useInternal = BrowserPreferences.isInternalBrowserEnabled(context)
        val activity = findActivity(context)

        if (useInternal && activity != null && !activity.isFinishing && !activity.isDestroyed) {
            try {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val currentFocus = activity.currentFocus
                if (currentFocus != null) {
                    imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                    currentFocus.clearFocus()
                }

                activity.window.decorView.post {
                    try {
                        if (!activity.isFinishing && !activity.isDestroyed && !activity.supportFragmentManager.isStateSaved) {
                            val dialog = BrowserBottomSheetDialogFragment.newInstance(targetUrl)
                            dialog.show(activity.supportFragmentManager, "BrowserBottomSheet")
                            Log.d("BrowserDebug", "Dialog.show called successfully with url: $targetUrl")
                        } else {
                            openExternalFallback(context, targetUrl)
                        }
                    } catch (e: Exception) {
                        Log.e("BrowserDebug", "Error showing browser dialog fragment", e)
                        openExternalFallback(context, targetUrl)
                    }
                }
                return
            } catch (e: Exception) {
                Log.e("BrowserDebug", "Failed to launch internal browser, falling back to system intent", e)
            }
        }

        openExternalFallback(context, targetUrl)
    }

    private fun findActivity(context: Context?): FragmentActivity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    private fun openExternalFallback(context: Context, targetUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("BrowserDebug", "Failed to open external browser", e)
        }
    }
}
