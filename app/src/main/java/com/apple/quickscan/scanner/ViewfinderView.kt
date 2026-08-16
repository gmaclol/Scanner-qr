package com.apple.quickscan.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class ViewfinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        strokeWidth = dpToPx(4f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dpToPx(3f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val frameRect = RectF()
    private var laserY = 0f
    private var laserAnimator: ValueAnimator? = null

    private val cornerLength = dpToPx(28f)
    private val cornerRadius = dpToPx(18f)

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = (minOf(w, h) * 0.72f).coerceAtMost(dpToPx(300f))
        val left = (w - size) / 2f
        val top = (h - size) / 2f - dpToPx(20f)
        frameRect.set(left, top, left + size, top + size)
        startLaserAnimation()
    }

    private fun startLaserAnimation() {
        laserAnimator?.cancel()
        laserAnimator = ValueAnimator.ofFloat(frameRect.top + cornerRadius, frameRect.bottom - cornerRadius).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                laserY = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw translucent dark mask outside the reticle
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, clearPaint)

        // Draw 4 Apple-style rounded corners
        drawCorners(canvas)

        // Draw glowing laser line
        val laserShader = LinearGradient(
            frameRect.left + dpToPx(16f), laserY,
            frameRect.right - dpToPx(16f), laserY,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#007AFF"),
                Color.parseColor("#5AC8FA"),
                Color.parseColor("#007AFF"),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        laserPaint.shader = laserShader
        canvas.drawLine(
            frameRect.left + dpToPx(16f),
            laserY,
            frameRect.right - dpToPx(16f),
            laserY,
            laserPaint
        )
    }

    private fun drawCorners(canvas: Canvas) {
        val l = frameRect.left
        val t = frameRect.top
        val r = frameRect.right
        val b = frameRect.bottom
        val cl = cornerLength
        val cr = cornerRadius

        // Top-Left Corner
        val pathTL = Path().apply {
            moveTo(l, t + cl)
            lineTo(l, t + cr)
            quadTo(l, t, l + cr, t)
            lineTo(l + cl, t)
        }
        canvas.drawPath(pathTL, cornerPaint)

        // Top-Right Corner
        val pathTR = Path().apply {
            moveTo(r - cl, t)
            lineTo(r - cr, t)
            quadTo(r, t, r, t + cr)
            lineTo(r, t + cl)
        }
        canvas.drawPath(pathTR, cornerPaint)

        // Bottom-Left Corner
        val pathBL = Path().apply {
            moveTo(l, b - cl)
            lineTo(l, b - cr)
            quadTo(l, b, l + cr, b)
            lineTo(l + cl, b)
        }
        canvas.drawPath(pathBL, cornerPaint)

        // Bottom-Right Corner
        val pathBR = Path().apply {
            moveTo(r - cl, b)
            lineTo(r - cr, b)
            quadTo(r, b, r, b - cr)
            lineTo(r, b - cl)
        }
        canvas.drawPath(pathBR, cornerPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        laserAnimator?.cancel()
    }

    private fun dpToPx(dp: Float): Float = dp * context.resources.displayMetrics.density
}
