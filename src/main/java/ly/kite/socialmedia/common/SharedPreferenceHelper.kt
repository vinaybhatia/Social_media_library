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

/**
 * Created by ITDIVISION\maximess139 on 10/3/18.
 */
object SharedPreferenceHelper {
    fun putBoolean(ct: Context, key: String?, value: Boolean) {
        val myPrefs = ct.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val prefsEditor = myPrefs.edit()
        prefsEditor.putBoolean(key, value)
        prefsEditor.commit()
    }

    fun getBoolean(ct: Context, key: String?): Boolean {
        val myPrefs =
            ct.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        return myPrefs.getBoolean(key, false)
    }
}