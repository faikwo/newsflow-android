package com.newsflow.ui.feed

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.newsflow.R

/**
 * ItemTouchHelper callback for swipe actions on article items.
 * - Swipe RIGHT → Like/Unlike (shows heart icon)
 * - Swipe LEFT → Save/Unsave (shows bookmark icon)
 * - Long swipe LEFT (past threshold) → Hide article (shows hide icon)
 */
class SwipeCallback(
    private val adapter: ArticleAdapter,
    private val onLike: (position: Int) -> Unit,
    private val onSave: (position: Int) -> Unit,
    private val onHide: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    companion object {
        private const val HIDE_THRESHOLD = 0.5f  // 50% swipe to trigger hide
    }

    private var longSwipeTriggered = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false  // We don't support drag-and-drop

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return

        when (direction) {
            ItemTouchHelper.RIGHT -> {
                onLike(position)
                adapter.notifyItemChanged(position)  // Reset the swipe
            }
            ItemTouchHelper.LEFT -> {
                if (longSwipeTriggered) {
                    onHide(position)
                } else {
                    onSave(position)
                }
                adapter.notifyItemChanged(position)  // Reset the swipe
            }
        }
        longSwipeTriggered = false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val itemView = viewHolder.itemView
            val width = itemView.width.toFloat()
            val height = itemView.height.toFloat()

            // Calculate swipe percentage
            val swipePercent = kotlin.math.abs(dX) / width

            // Track long swipe for left direction
            if (dX < 0) {
                longSwipeTriggered = swipePercent >= HIDE_THRESHOLD
            }

            // Draw background and icon based on swipe direction
            if (dX > 0) {
                // Swiping RIGHT - Like action
                drawRightSwipeBackground(c, itemView, dX, width, height)
            } else if (dX < 0) {
                // Swiping LEFT - Save or Hide
                if (longSwipeTriggered) {
                    drawLeftSwipeHideBackground(c, itemView, dX, width, height)
                } else {
                    drawLeftSwipeSaveBackground(c, itemView, dX, width, height)
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun drawRightSwipeBackground(
        c: Canvas,
        itemView: View,
        dX: Float,
        width: Float,
        height: Float
    ) {
        val background = RectF(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            itemView.left + dX,
            itemView.bottom.toFloat()
        )

        val paint = Paint().apply { color = ContextCompat.getColor(itemView.context, R.color.like_color) }
        c.drawRect(background, paint)

        // Draw heart icon
        drawIcon(c, itemView, R.drawable.ic_thumb_up, dX, width, height, isLeft = false)
    }

    private fun drawLeftSwipeSaveBackground(
        c: Canvas,
        itemView: View,
        dX: Float,
        width: Float,
        height: Float
    ) {
        val background = RectF(
            itemView.right + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )

        val paint = Paint().apply { color = ContextCompat.getColor(itemView.context, R.color.save_color) }
        c.drawRect(background, paint)

        // Draw bookmark icon
        drawIcon(c, itemView, R.drawable.ic_bookmark, dX, width, height, isLeft = true)
    }

    private fun drawLeftSwipeHideBackground(
        c: Canvas,
        itemView: View,
        dX: Float,
        width: Float,
        height: Float
    ) {
        val background = RectF(
            itemView.right + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )

        val paint = Paint().apply { color = ContextCompat.getColor(itemView.context, R.color.hide_color) }
        c.drawRect(background, paint)

        // Draw hide icon
        drawIcon(c, itemView, R.drawable.ic_hide, dX, width, height, isLeft = true)
    }

    private fun drawIcon(
        c: Canvas,
        itemView: View,
        iconResId: Int,
        dX: Float,
        width: Float,
        height: Float,
        isLeft: Boolean
    ) {
        val icon: Drawable? = ContextCompat.getDrawable(itemView.context, iconResId)
        icon?.let {
            val iconSize = (height * 0.4).toInt()
            val margin = (height * 0.3).toInt()

            val left: Int
            val right: Int

            if (isLeft) {
                // Icon on the right side (for left swipe)
                right = itemView.right - margin
                left = right - iconSize
            } else {
                // Icon on the left side (for right swipe)
                left = itemView.left + margin
                right = left + iconSize
            }

            val top = itemView.top + ((height - iconSize) / 2).toInt()
            val bottom = top + iconSize

            it.setBounds(left, top, right, bottom)
            it.setTint(Color.WHITE)
            it.draw(c)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        // Use a small threshold for normal swipe, but track the long swipe separately
        return 0.2f
    }
}