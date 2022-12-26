/*****************************************************
 *
 * FacebookPhotoPickerActivity.java
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

import ly.kite.socialmedia.common.UIUtil.dismissDialog
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker.Companion.getFbAccessToken
import android.app.Activity
import android.content.Intent
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent.IPhotosCallback
import ly.kite.socialmedia.facebookphotopicker.FacebookAgent.IAlbumsCallback
import android.os.Bundle
import com.facebook.FacebookCallback
import com.facebook.login.LoginResult
import ly.kite.socialmedia.instagramphotopicker.InstagramPhotoPicker
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
import android.app.AlertDialog
import android.app.Dialog
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPickerActivity
import android.os.Parcelable
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPicker
import ly.kite.socialmedia.R
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity
import ly.kite.socialmedia.facebookphotopicker.NewFacebookLoginActivity.facebookLoginListener
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import ly.kite.socialmedia.facebookphotopicker.FacebookPhotoPickerActivity.RetryListener
import android.content.DialogInterface
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.Fragment
import ly.kite.socialmedia.common.*
import ly.kite.socialmedia.common.Constant.FACEBOOK_ALBUMS
import java.io.IOException
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

///// Import(s) /////
///// Class Declaration /////
/*****************************************************
 *
 * This activity is the Facebook photo picker.
 *
 */
class FacebookPhotoPickerActivity : Activity(), IPhotosCallback, IAlbumsCallback {
    ////////// Static Variable(s) //////////
    ////////// Member Variable(s) //////////
    private var mFacebookAgent: FacebookAgent? = null
    private var photoId: Long = -1
    private var morePHotos = false
    private val selectedAlbumName: String? = null
    private val dialog: Dialog? = null
    ////////// Constructor(s) //////////
    ////////// Activity Method(s) //////////
    /*****************************************************
     *
     * Called when the activity is created.
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Get the Facebook agent
        mFacebookAgent = FacebookAgent.getInstance(this);
        try {
            val info =
                packageManager.getPackageInfo(this.packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(
                    "KeyHash", "KeyHash:" + Base64.encodeToString(
                        md.digest(),
                        Base64.DEFAULT
                    )
                )
                //                Toast.makeText(getApplicationContext(), Base64.encodeToString(md.digest(),
//                        Base64.DEFAULT), Toast.LENGTH_LONG).show();
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }

        // dialog = UIUtil.showProgressDialogWithText(this);
        dataFromBundle
        if (photoId < 0) {
            val fbAccessToken = getFbAccessToken(this)
            if (fbAccessToken != null) {
                val list = getFbAlbumList(Constant.SOCIAL_MEDIA_ALBUM_NAME)
                val intent = Intent()
                intent.putExtra(FACEBOOK_ALBUMS, list)
                intent.putExtra("AccessToken", fbAccessToken)
                setResult(RESULT_OK, intent)
                dismissDialog(dialog)
                finish()
            }
        } else {
            dismissDialog(dialog)
            finish()
        }
        if (photoId == -1L) {
            //   UIUtil.dismissDialog(dialog);
            displayAlbums()
        } else {
            displayGallery()
        }
    }

    private fun displayAlbums() {
        if (mFacebookAgent != null) {
            mFacebookAgent!!.resetPhotos()
            mFacebookAgent!!.getAlbums(this)
        }
    }

    private fun getFbAlbumList(key: String): ArrayList<Album>? {
        var list: ArrayList<Album>? = null
        val prefs = getSharedPreferences("myPrefs", 0)
        // SharedPreferences.Editor editor = prefs.edit();
        val gson = Gson()
        val jsonText = prefs.getString(key, null)
        val arrayUrl = gson.fromJson(jsonText, Array<Album>::class.java)
        if (arrayUrl != null) {
            list = ArrayList(Arrays.asList(*arrayUrl))
        }
        return list
    }

    private val dataFromBundle: Unit
        private get() {
            val bundle = intent.extras
            if (bundle != null) {
                photoId = bundle.getLong(Constant.FACEBOOK_PHOTO_ID)
                morePHotos = bundle.getBoolean("MorePhotos", false)
            }
        }

    /*****************************************************
     *
     * Called when an activity returns a result.
     *
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        mFacebookAgent!!.onActivityResult(requestCode, resultCode, data)
    }

    /*****************************************************
     *
     * Called when an item in the options menu is selected.
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // See what menu item was selected
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            ///// Home /////

            // We intercept the home button and do the same as if the
            // back key had been pressed.
            super.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    ////////// FacebookAgent.PhotosCallback Method(s) //////////
    /*****************************************************
     *
     * Called when photos were successfully retrieved.
     *
     */
    override fun facOnPhotosSuccess(photoList: ArrayList<Photo?>?, morePhotos: Boolean, id: Long) {
        //mPagingGridView.onFinishLoading(morePhotos, photoList);
        val i = Intent()
        i.putExtra(Constant.FACEBOOK_PHOTOS, photoList)
        i.putExtra("MorePhotos", morePhotos)
        i.putExtra("id", photoId)
        setResult(RESULT_OK, i)
        dismissDialog(dialog)
        finish()
    }

    /*****************************************************
     *
     * Called when there was an error retrieving photos.
     *
     */
    override fun facOnError(exception: Exception?) {
        Log.e(LOG_TAG, "Facebook error", exception)
        val retryListener = RetryListener()
        val cancelListener: CancelListener = CancelListener()
        if (exception is IOException) {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.title_facebook_alert_dialog)
                .setMessage(
                    getString(
                        R.string.message_facebook_alert_dialog,
                        getString(R.string.please_connect_to_internet)
                    )
                )
                .setPositiveButton(android.R.string.ok, cancelListener)
                .setOnCancelListener(cancelListener)
                .create()
                .show()
        } else {
            setResult(RESULT_CANCELED)
            // UIUtil.dismissDialog(dialog);
            finish()
        }
    }

    /*****************************************************
     *
     * Called when photo retrieval was cancelled.
     *
     */
    override fun facOnCancel() {
        setResult(RESULT_CANCELED)
        //  UIUtil.dismissDialog(dialog);
        finish()
    }
    ////////// PagingGridView.Pagingable Method(s) //////////
    /*****************************************************
     *
     * Called when the done action is clicked.
     *
     */
    /* @Override
    public void onLoadMoreItems() {
        mFacebookAgent.getPhotos(this, photoId);
    }*/
    ////////// MultiChoiceModeListener.IListener Method(s) //////////
    /*****************************************************
     *
     * Called when the done action is clicked.
     *
     */
    /* @Override
    public void mcmlOnAction(Photo[] photoArray) {
        // Set the result and exit

        Intent resultIntent = new Intent();

        resultIntent.putExtra(FacebookPhotoPicker.EXTRA_SELECTED_PHOTOS, photoArray);

        setResult(Activity.RESULT_OK, resultIntent);

        finish();
    }*/
    ////////// Method(s) //////////
    /*****************************************************
     *
     * Displays the gallery.
     *
     */
    private fun displayGallery() {
        /*if (mPagingBaseAdaptor != null) {
            mPagingBaseAdaptor.removeAllItems();
        }*/
        if (mFacebookAgent != null) {
            if (!morePHotos) {
                mFacebookAgent!!.resetPhotos()
            }
            mFacebookAgent!!.getPhotos(this, photoId, false, null)
        }
    }

    override fun facOnAlbumsSuccess(albumList: ArrayList<Album>?, morePhotos: Boolean) {
        val fbAccessToken = getFbAccessToken(this)
        val i = Intent()
        i.putExtra("AccessToken", fbAccessToken)
        i.putExtra(FACEBOOK_ALBUMS, albumList)
        setResult(RESULT_OK, i)
        //UIUtil.dismissDialog(dialog);
        finish()
    }
    ////////// Inner Class(es) //////////
    /*****************************************************
     *
     * The alert dialog retry button listener.
     *
     */
    private inner class RetryListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            displayGallery()
        }
    }

    /*****************************************************
     *
     * The alert dialog cancel (button) listener.
     *
     */
    private inner class CancelListener : DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            finish()
        }

        override fun onCancel(dialog: DialogInterface) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mFacebookAgent!!.initAgent()
    }

    companion object {
        ////////// Static Constant(s) //////////
        private const val LOG_TAG = "FacebookPhotoPickerA..."
        ////////// Static Initialiser(s) //////////
        ////////// Static Method(s) //////////
        /*****************************************************
         *
         * Starts this activity.
         *
         */
        fun startForResult(fragment: Fragment, activityRequestCode: Int) {
            val intent = Intent(fragment.activity, FacebookPhotoPickerActivity::class.java)
            fragment.startActivityForResult(intent, activityRequestCode)
        }
    }
}