package ly.kite.socialmedia.common

import android.graphics.drawable.ColorDrawable
import ly.kite.socialmedia.R
import ly.kite.socialmedia.common.LoadingView
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import ly.kite.socialmedia.common.DeviceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.os.Build
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View

class LoadingView : LinearLayout {
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        val view = inflate(getContext(), R.layout.loading_view, this)
        val loadingText = view.findViewById<View>(R.id.video_item_label) as TextView
        val splashTimer: Thread = object : Thread() {
            override fun run() {
                try {
                    var splashTime = 0
                    while (splashTime < 4000) {
                        sleep(100)
                        var loading = ""
                        if (splashTime < 1000) {
                            loading = context.getString(R.string.loading)
                            setText(context, loading, loadingText)
                        } else if (splashTime >= 1000 && splashTime < 2000) {
                            loading = context.getString(R.string.loading) + " ."
                            setText(context, loading, loadingText)
                        } else if (splashTime >= 2000 && splashTime < 3000) {
                            loading = context.getString(R.string.loading) + " .."
                            setText(context, loading, loadingText)
                        } else if (splashTime >= 3000) {
                            loading = context.getString(R.string.loading) + " ..."
                            setText(context, loading, loadingText)
                            splashTime = 0
                        }
                        splashTime = splashTime + 100
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        splashTimer.start()
    }

    private fun setText(context: Context, text: CharSequence, textView: TextView) {
        (context as Activity).runOnUiThread { textView.text = text }
    }
}