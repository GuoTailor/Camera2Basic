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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.android.camera.utils.decodeExifOrientation
import com.example.android.camera2.basic.databinding.ImageViewerBinding
import java.io.BufferedInputStream
import java.io.File
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

    private var imageViewerBinding: ImageViewerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        imageViewerBinding = ImageViewerBinding.inflate(inflater, container, false)
        imageViewerBinding!!.send.setOnClickListener {
            Log.i(TAG, "onCreateView: >>>>>>>>>>")
            imageViewerBinding
        }
        return imageViewerBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load input image file
        val inputBuffer = loadInputBuffer()

        // Load the main JPEG image
        val item = decodeBitmap(inputBuffer, 0, inputBuffer.size)
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

    /** Utility function used to read input file into a byte array */
    private fun loadInputBuffer(): ByteArray {
        val inputFile = File(args.filePath)
        return BufferedInputStream(inputFile.inputStream()).let { stream ->
            ByteArray(stream.available()).also {
                stream.read(it)
                stream.close()
            }
        }
    }

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
        private const val DOWNSAMPLE_SIZE: Int = 1024  // 1MP

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
