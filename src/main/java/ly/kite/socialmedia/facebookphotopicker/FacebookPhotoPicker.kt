/*****************************************************
 *
 * FacebookPhotoPicker.java
 *
 *
 * Modified MIT License
 *
 * Copyright (c) 2010-2015 Kite Tech Ltd. https://www.kite.ly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The software MAY ONLY be used with the Kite Tech Ltd platform and MAY NOT be modified
 * to be used with any competitor platforms. This means the software MAY NOT be modified
 * to place orders with any competitors to Kite Tech Ltd, all orders MUST go through the
 * Kite Tech Ltd platform servers.
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
///// Package Declaration /////
package ly.kite.socialmedia.facebookphotopicker

import android.content.Intent
import com.facebook.login.LoginResult
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker
import ly.kite.socialmedia.common.DeviceManager
import com.facebook.FacebookException
import com.facebook.GraphResponse
import com.facebook.FacebookRequestError
import org.json.JSONObject
import org.json.JSONArray
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent.HandleFbAlumsTask
import org.json.JSONException
import android.os.AsyncTask
import com.facebook.GraphResponse.PagingDirection
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlin.Throws
import android.annotation.SuppressLint
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPickerActivity
import android.os.Parcelable
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPicker
import ly.kite.socialmedia.R
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity.facebookLoginListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.DialogInterface
import androidx.fragment.app.Fragment
import ly.kite.socialmedia.common.Photo

///// Import(s) /////
///// Class Declaration /////
/*****************************************************
 *
 * This class is the gateway to the Facebook photo picker
 * functionality.
 *
 */
object FacebookPhotoPicker {
    ////////// Static Constant(s) //////////
    private const val LOG_TAG = "FacebookPhotoPicker"
    const val EXTRA_SELECTED_PHOTOS = "ly.kite.facebookphotopicker.EXTRA_SELECTED_PHOTOS"
    ////////// Static Variable(s) //////////
    ////////// Member Variable(s) //////////
    ////////// Static Initialiser(s) //////////
    ////////// Static Method(s) //////////
    /*****************************************************
     *
     * Starts the Facebook photo picker.
     *
     */
    fun startPhotoPickerForResult(fragment: Fragment, activityRequestCode: Int) {
        FacebookPhotoPickerActivity.Companion.startForResult(fragment, activityRequestCode)
    }

    /*****************************************************
     *
     * Returns an array of picked photos.
     *
     */
    fun getResultPhotos(data: Intent): Array<Photo?> {
        val photos = data.getParcelableArrayExtra(EXTRA_SELECTED_PHOTOS)
        val facebookPhotos = arrayOfNulls<Photo>(
            photos!!.size
        )
        System.arraycopy(photos, 0, facebookPhotos, 0, photos.size)
        return facebookPhotos
    } ////////// Constructor(s) //////////
    ////////// Method(s) //////////
    ////////// Inner Class(es) //////////
}