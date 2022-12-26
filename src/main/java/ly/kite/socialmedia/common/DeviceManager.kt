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
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import ly.kite.socialmedia.BuildConfig
import java.io.*
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

/**
 * Created by ITDIVISION\maximess139 on 9/2/18.
 */
object DeviceManager {
    /*public static void deleteTempFolder(String directoryName){
        File fileOrDirectory = new File(Environment.getExternalStorageDirectory()+directoryName);
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
            {
                child.delete();
                deleteTempFolder(child.getName());
            }

        fileOrDirectory.delete();
    }*/
    @JvmStatic
    fun deleteFileAndFolderFromInternalStorage(context: Context, directoryName: String) {
        val fileOrDirectory =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM).toString() + directoryName)
        //        File fileOrDirectory = new File(Environment.getExternalStorageDirectory()+directoryName);
        deleteRecursive(fileOrDirectory)
    }

    @JvmStatic
    fun deleteFileAndFolderFromAppCache(context: Context, directoryName: String) {
        val fileOrDirectory = File(context.externalCacheDir.toString() + directoryName)
        deleteRecursive(fileOrDirectory)
    }

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    fun deleteSdFiles(photoFilePath: String?) {
        val fileOrDirectory = File(photoFilePath)
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    fun saveJson(`object`: Any?, type: Type?, directory: String?, fileName: String?) {
        val newFolder = File(Environment.getExternalStorageDirectory(), directory)
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        }
        val file = File(newFolder, fileName)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                Log.e("ERROR: ", e.message!!)
            }
        }

        /*File file = new File(getApplicationContext().getDir(directory, Context.MODE_PRIVATE),
                fileName);*/
        var outputStream: OutputStream? = null
        val gson = GsonBuilder().setPrettyPrinting()
            .create()
        try {
            outputStream = FileOutputStream(file)
            val bufferedWriter: BufferedWriter
            bufferedWriter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                BufferedWriter(
                    OutputStreamWriter(
                        outputStream,
                        StandardCharsets.UTF_8
                    )
                )
            } else {
                BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
            }
            gson.toJson(`object`, type, bufferedWriter)
            bufferedWriter.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            if (BuildConfig.DEBUG) Log.e("saveJson", "saveUserData, FileNotFoundException e: '$e'")
        } catch (e: IOException) {
            e.printStackTrace()
            if (BuildConfig.DEBUG) Log.e("saveJson", "saveUserData, IOException e: '$e'")
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush()
                    outputStream.close()
                } catch (e: IOException) {
                    if (BuildConfig.DEBUG) Log.e("saveJson", "saveUserData, finally, e: '$e'")
                }
            }
        }
    }

    fun loadJson(type: Type?, directory: String, fileName: String?): Any? {
        val file = File(Environment.getExternalStorageDirectory().toString() + directory, fileName)
        if (file.exists()) {
            var jsonData: Any? = null

            /*File file = new File(getApplicationContext().getDir(directory, Context.MODE_PRIVATE),
                    fileName);*/
            var inputStream: InputStream? = null
            val gson = GsonBuilder().setPrettyPrinting()
                .create()
            try {
                inputStream = FileInputStream(file)
                val streamReader: InputStreamReader
                streamReader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                    )
                } else {
                    InputStreamReader(inputStream, "UTF-8")
                }
                jsonData = gson.fromJson(streamReader, type)
                streamReader.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                if (BuildConfig.DEBUG) Log.e(
                    ContentValues.TAG,
                    "loadJson, FileNotFoundException e: '$e'"
                )
            } catch (e: IOException) {
                e.printStackTrace()
                if (BuildConfig.DEBUG) Log.e(ContentValues.TAG, "loadJson, IOException e: '$e'")
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        if (BuildConfig.DEBUG) Log.e(
                            ContentValues.TAG,
                            "loadJson, finally, e: '$e'"
                        )
                    }
                }
            }
            return jsonData
        }
        return null
    }

    fun openSettings(activityContext: Activity, requestCode: Int) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activityContext.packageName, null)
        intent.data = uri
        activityContext.startActivityForResult(intent, requestCode)
    }
}