package com.sircoolmind.docsnaplocal.scan

import android.graphics.Bitmap
import com.sircoolmind.docsnaplocal.geometry.Vec2
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import kotlin.math.min

/** Finds a document's Corners in a photo and Flattens it, with OpenCV. */
object DocumentScanner {
    private const val DETECT_SIDE = 800f
    private const val MIN_AREA_FRACTION = 0.2

    private val loaded by lazy { OpenCVLoader.initLocal() }

    /** Corners of the largest page-like outline, ordered, in [photo]'s pixels; null if none. */
    fun findCorners(photo: Bitmap): List<Vec2>? {
        check(loaded) { "OpenCV failed to load" }
        val scale = min(1f, DETECT_SIDE / max(photo.width, photo.height))
        val small = Bitmap.createScaledBitmap(
            photo.argb(),
            (photo.width * scale).toInt().coerceAtLeast(1),
            (photo.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        val rgba = Mat()
        val gray = Mat()
        val edges = Mat()
        val binary = Mat()
        try {
            Utils.bitmapToMat(small, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
            val minArea = MIN_AREA_FRACTION * small.width * small.height

            // Edges catch most pages; a brightness threshold catches white paper on a busy table.
            Imgproc.Canny(gray, edges, 50.0, 150.0)
            Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))
            val quad = largestQuad(edges, minArea) ?: run {
                Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
                largestQuad(binary, minArea)
            }
            return quad?.map { Vec2(it.x.toFloat() / scale, it.y.toFloat() / scale) }?.let(::orderCorners)
        } finally {
            listOf(rgba, gray, edges, binary).forEach { it.release() }
        }
    }

    private fun largestQuad(binary: Mat, minArea: Double): List<Point>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        try {
            return contours
                .map { it to Imgproc.contourArea(it) }
                .filter { it.second >= minArea }
                .sortedByDescending { it.second }
                .take(10)
                .firstNotNullOfOrNull { (contour, _) ->
                    val curve = MatOfPoint2f(*contour.toArray())
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(curve, approx, 0.02 * Imgproc.arcLength(curve, true), true)
                    val points = approx.toArray()
                    curve.release()
                    approx.release()
                    points.takeIf { it.size == 4 && Imgproc.isContourConvex(MatOfPoint(*it)) }?.toList()
                }
        } finally {
            contours.forEach { it.release() }
        }
    }

    /** Warps the area inside [corners] of [photo] into a flat, rectangular page. */
    fun flatten(photo: Bitmap, corners: List<Vec2>): Bitmap {
        check(loaded) { "OpenCV failed to load" }
        val ordered = orderCorners(corners)
        val (width, height) = flattenedSize(ordered)
        val w = width.toDouble()
        val h = height.toDouble()
        val src = MatOfPoint2f(*ordered.map { Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray())
        val dst = MatOfPoint2f(Point(0.0, 0.0), Point(w, 0.0), Point(w, h), Point(0.0, h))
        val input = Mat()
        val output = Mat()
        val transform = Imgproc.getPerspectiveTransform(src, dst)
        try {
            Utils.bitmapToMat(photo.argb(), input)
            Imgproc.warpPerspective(input, output, transform, Size(w, h), Imgproc.INTER_LINEAR)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { Utils.matToBitmap(output, it) }
        } finally {
            listOf(src, dst, input, output, transform).forEach { it.release() }
        }
    }

    private fun Bitmap.argb(): Bitmap =
        if (config == Bitmap.Config.ARGB_8888) this else copy(Bitmap.Config.ARGB_8888, false)
}
