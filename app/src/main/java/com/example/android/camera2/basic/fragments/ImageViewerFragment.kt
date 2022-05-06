/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2.basic.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.example.android.camera.utils.decodeExifOrientation
import com.example.android.camera2.basic.databinding.ImageViewerBinding
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class ImageViewerFragment : Fragment() {

    /** AndroidX navigation arguments */
    private val args: ImageViewerFragmentArgs by navArgs()

    /** Default Bitmap decoding options */
    private val bitmapOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        // Keep Bitmaps at less than 1 MP
        if (max(outHeight, outWidth) > DOWNSAMPLE_SIZE) {
            val scaleFactorX = outWidth / DOWNSAMPLE_SIZE + 1
            val scaleFactorY = outHeight / DOWNSAMPLE_SIZE + 1
            inSampleSize = max(scaleFactorX, scaleFactorY)
        }
        inMutable = true
    }

    /** Bitmap transformation derived from passed arguments */
    private val bitmapTransformation: Matrix by lazy { decodeExifOrientation(args.orientation) }

    /** Flag indicating that there is depth data available for this image */
    private val isDepth: Boolean by lazy { args.depth }

    /** Data backing our Bitmap viewpager */
    private var bitmap: Bitmap? = null

    private val sharedPref: SharedPreferences by lazy { requireActivity().getPreferences(Context.MODE_PRIVATE) }

    private var imageViewerBinding: ImageViewerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageViewerBinding = ImageViewerBinding.inflate(inflater, container, false)
        imageViewerBinding!!.send.setOnClickListener {
            Log.i(TAG, "onCreateView: >>>>>>>>>>")
            val bitmap = imageViewerBinding!!.signView.mBitmap
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault())
//            bitmap!!.saveToAlbum(it.context, "IMG_${sdf.format(Date())}.jpg", null, 100)
            openDialog(it.context)
        }
        imageViewerBinding!!.back.setOnClickListener {
            NavHostFragment.findNavController(this)
                .popBackStack();
        }
        return imageViewerBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load input image file
//        val inputBuffer = loadInputBuffer()

        // Load the main JPEG image
        val item = args.bitmap
        bitmap = item
        val resources = this.resources
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels
        val screenHeight = dm.heightPixels
        Log.i(TAG, "onCreateView: " + item.width + " " + item.height)
        Log.i(TAG, "onViewCreated: $screenWidth $screenHeight")

        imageViewerBinding!!.signView.prepare(item, screenWidth, screenHeight)

        view.post {
            imageViewerBinding!!.image.setImageBitmap(item)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        imageViewerBinding!!.signView.clear()
    }

    private fun openDialog(context: Context) {
        val inputServer = EditText(context)
        inputServer.setText(sharedPref.getString("ip", "192.168.0.104"))
        val builder = AlertDialog.Builder(context);
        builder.setTitle("发送").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
            .setNegativeButton("取消", null);
        builder.setPositiveButton("确认") { _, _ ->
            val ip = inputServer.text.toString()
            Log.d(TAG, "openDialog: $ip")
            with(sharedPref.edit()) {
                putString("ip", ip)
                apply()
            }
            Thread{
                upload(ip)
            }.start()
        }
        builder.show()
    }

    fun upload(ip: String) {
        val boundary = UUID.randomUUID().toString()
        val PRE_FIX = "\r\n--$boundary\r\n" //定义开始
        val END_FIX = "\r\n--$boundary--\r\n"  //定义结束
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.getDefault())
        val url = URL("http://$ip/ISAPI/Intelligent/FDLib/pictureUpload")
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.doOutput = true
        conn.doInput = true
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary");
        conn.setRequestProperty("connection", "Keep-Alive")
        conn.setRequestProperty("Charsert", "UTF-8")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        conn.useCaches = false

        conn.connect()
        val outputStream = DataOutputStream(conn.outputStream)
        val strBuf = StringBuffer()
        // 标识payLoad + 文件流的起始位置
        strBuf.append(PRE_FIX)
            .append("Content-Disposition: form-data; name=\"importImage\"; filename=IMG_${sdf.format(Date())}.jpg\r\n")
            .append("Content-Type: application/octet-stream" + "\r\n")
            .append("\r\n") //留一个空行
        outputStream.write(strBuf.toString().toByteArray())
        val format = Bitmap.CompressFormat.JPEG
        imageViewerBinding!!.signView.mBitmap!!.compress(format, 100, outputStream)
        strBuf.setLength(0)
        strBuf.append(PRE_FIX)
            .append("Content-Disposition: form-data; name=\"FaceAppendData\"\r\n")
            .append("Content-Type: text/plain; charset=utf-8\r\n")
            .append("\r\n")
            .append("<?xml version='1.0' encoding='UTF-8'?><PictureUploadData><FDID>69369AD6FF1546018FFECED3B7B3AEAE</FDID><FaceAppendData><name>test</name><RegionCoordinatesList><RegionCoordinates><positionX>258</positionX><positionY>307</positionY></RegionCoordinates><RegionCoordinates><positionX>750</positionX><positionY>307</positionY></RegionCoordinates><RegionCoordinates><positionX>750</positionX><positionY>664</positionY></RegionCoordinates><RegionCoordinates><positionX>258</positionX><positionY>664</positionY></RegionCoordinates></RegionCoordinatesList><bornTime>2004-01-01</bornTime><sex>male</sex><certificateType>ID</certificateType><certificateNumber></certificateNumber><PersonInfoExtendList><PersonInfoExtend><id>1</id><enable>true</enable><name>test</name><value></value></PersonInfoExtend></PersonInfoExtendList></FaceAppendData></PictureUploadData>")
        outputStream.write(strBuf.toString().toByteArray())
        outputStream.write(END_FIX.toByteArray())
        outputStream.flush()
//        outputStream.close()
        val responseCode: Int = conn.responseCode
        if (responseCode == 200) {
            val readBytes = conn.inputStream.readBytes()
            val result  = String(readBytes)
            Log.d(TAG, "upload: $result")
            conn.inputStream.close()
        }
        conn.disconnect()
    }

    /** Utility function used to read input file into a byte array */
    /*private fun loadInputBuffer(): ByteArray {
        val inputFile = File(args.filePath)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }*/

    /** Utility function used to decode a [Bitmap] from a byte array */
    private fun decodeBitmap(buffer: ByteArray, start: Int, length: Int): Bitmap {

        // Load bitmap from given buffer
        val bitmap = BitmapFactory.decodeByteArray(buffer, start, length, bitmapOptions)

        // Transform bitmap orientation using provided metadata
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, bitmapTransformation, true
        )
    }

    companion object {
        private val TAG = ImageViewerFragment::class.java.simpleName

        /** Maximum size of [Bitmap] decoded */
        const val DOWNSAMPLE_SIZE: Int = 1024  // 1MP

        /** These are the magic numbers used to separate the different JPG data chunks */
        private val JPEG_DELIMITER_BYTES = arrayOf(-1, -39)

        /**
         * Utility function used to find the markers indicating separation between JPEG data chunks
         */
        private fun findNextJpegEndMarker(jpegBuffer: ByteArray, start: Int): Int {

            // Sanitize input arguments
            assert(start >= 0) { "Invalid start marker: $start" }
            assert(jpegBuffer.size > start) {
                "Buffer size (${jpegBuffer.size}) smaller than start marker ($start)"
            }

            // Perform a linear search until the delimiter is found
            for (i in start until jpegBuffer.size - 1) {
                if (jpegBuffer[i].toInt() == JPEG_DELIMITER_BYTES[0] &&
                    jpegBuffer[i + 1].toInt() == JPEG_DELIMITER_BYTES[1]
                ) {
                    return i + 2
                }
            }

            // If we reach this, it means that no marker was found
            throw RuntimeException("Separator marker not found in buffer (${jpegBuffer.size})")
        }
    }
}
