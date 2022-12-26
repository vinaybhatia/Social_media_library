package ly.kite.socialmedia.common

import android.graphics.drawable.ColorDrawable
import ly.kite.socialmedia.R
import ly.kite.socialmedia.common.LoadingView
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import android.app.Dialog
import ly.kite.socialmedia.common.DeviceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.os.Build
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Window
import java.lang.Exception

/**
 * Created by ITDIVISION\maximess139 on 6/2/18.
 */
object UIUtil {
    fun showProgressDialog(activityContext: Context?, isCancelable: Boolean): Dialog? {
        var dialog: Dialog? = null
        try {
            dialog = Dialog(activityContext!!)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setContentView(R.layout.async_progress_dialog_layout)
            dialog.setCancelable(isCancelable)
            if (!dialog.isShowing) dialog.show()
        } catch (exception: Exception) {
            //Consume it
            exception.printStackTrace()
        }
        return dialog
    }

    @JvmStatic
    fun showProgressDialogWithText(activityContext: Context?): Dialog? {
        var dialog: Dialog? = null
        if (activityContext != null) {
            try {
                dialog = Dialog(activityContext)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                //WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.setContentView(LoadingView(activityContext))
                dialog.setCancelable(false)
                dialog.show()
            } catch (exception: Exception) {
                //Consume it
                exception.printStackTrace()
            }
        }
        return dialog
    }

    @JvmStatic
    fun dismissDialog(dialog: Dialog?) {
        try {
            dialog?.dismiss()
        } catch (exception: Exception) {
            //Consume it
        }
    }
}