package me.hufman.androidautoidrive.phoneui.viewmodels

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.animation.ArgbEvaluatorCompat
import me.hufman.androidautoidrive.phoneui.ViewHelpers.visible
import me.hufman.androidautoidrive.phoneui.getThemeColor
import me.hufman.androidautoidrive.utils.Utils.getIconMask
import java.util.*
import kotlin.math.max


@BindingAdapter("android:src")
fun setImageViewBitmap(view: ImageView, bitmap: Bitmap?) {
	view.setImageBitmap(bitmap)
}
@BindingAdapter("android:src")
fun setImageViewResource(view: ImageView, resource: Int) {
	view.setImageResource(resource)
}
@BindingAdapter("android:src")
fun setImageViewResource(view: ImageView, drawable: Context.() -> Drawable?) {
	view.setImageDrawable(view.context.run(drawable))
}
@BindingAdapter("navigationIcon")
fun setToolbarIcon(view: Toolbar, drawable: Drawable?) {
	view.navigationIcon = drawable
}

@BindingAdapter("android:visibility")
fun setViewVisibility(view: View, visible: Boolean) {
	view.visible = visible
}
@BindingAdapter("android:visibility")
fun setViewVisibility(view: View, value: String) {
	view.visible = value.isNotBlank()
}

/**
 * Finds the index of the given item in an Adapter
 * Which aren't normally iterable, so now they are
 * May return -1 if the item doesn't seem to exist
 */
fun <T> Adapter.indexOf(item: T): Int {
	for (i in 0 until count) {
		if (getItem(i) == item) {
			return i
		}
	}
	return -1
}

// Set up the spinner based on the selectedValue from the LiveData
@BindingAdapter("selectedValue")
fun setSelectedValue(spinner: Spinner, selectedValue: String) {
	val position = spinner.adapter.indexOf(selectedValue)
	spinner.setSelection(max(0, position))
}
// Set up the spinner's event listeners
@BindingAdapter("selectedValueAttrChanged")
fun setInverseBindingListener(spinner: Spinner, inverseBindingListener: InverseBindingListener?) {
	if (inverseBindingListener == null) {
		spinner.onItemSelectedListener = null
	} else {
		spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				inverseBindingListener.onChange()
			}
			override fun onNothingSelected(parent: AdapterView<*>?) {}
		}
	}
}
// Triggered when the event listener fires from above
@InverseBindingAdapter(attribute="selectedValue", event="selectedValueAttrChanged")
fun getSelectedValue(spinner: Spinner): String {
	return spinner.selectedItem.toString()
}

// Dynamic text
@BindingAdapter("android:text")
fun setText(view: TextView, value: (Context.() -> String)?) {
	view.text = if (value != null) {
		view.context.run(value)
	} else {
		""
	}
}

// Dynamic text
@BindingAdapter("android:visibility")
fun setVisibilityByTextGetter(view: View, value: (Context.() -> String)?) {
	val text = if (value != null) {
		view.context.run(value)
	} else {
		""
	}
	view.visible = text.isNotBlank()
}

// Dynamic color with a smooth transition
@BindingAdapter("android:backgroundTint")
fun setBackgroundTint(view: View, value: (Context.() -> Int)?) {
	value ?: return
	val color = view.context.run(value)
	setBackgroundTint(view, color)
}
@BindingAdapter("android:backgroundTint")
fun setBackgroundTint(view: View, color: Int) {
	val startColor = view.backgroundTintList?.defaultColor
	if (startColor != color) {
		if (startColor == null) {
			view.backgroundTintList = ColorStateList.valueOf(color)
		} else {
			ValueAnimator.ofObject(ArgbEvaluatorCompat(), startColor, color).apply {
				addUpdateListener { view.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int) }
				start()
			}
		}
	}
}
@BindingAdapter("android:foregroundTint")
fun setForegroundTint(view: View, value: (Context.() -> Int)?) {
	value ?: return
	val color = view.context.run(value)
	setForegroundTint(view, color)
}
@BindingAdapter("android:foregroundTint")
fun setForegroundTint(view: View, color: Int) {
	val startColor = view.foregroundTintList?.defaultColor
	if (startColor != color) {
		if (startColor == null) {
			view.foregroundTintList = ColorStateList.valueOf(color)
		} else {
			ValueAnimator.ofObject(ArgbEvaluatorCompat(), startColor, color).apply {
				addUpdateListener { view.foregroundTintList = ColorStateList.valueOf(it.animatedValue as Int) }
				start()
			}
		}
	}
}
@BindingAdapter("tint")
fun setImageTint(view: ImageView, value: (Context.() -> Int)?) {
	value ?: return
	val color = view.context.run(value)
	setImageTint(view, color)
}
@BindingAdapter("tint")
fun setImageTint(view: ImageView, color: Int) {
	val startColor = view.imageTintList?.defaultColor
	if (startColor != color) {
		if (startColor == null) {
			view.imageTintList = ColorStateList.valueOf(color)
		} else {
			ValueAnimator.ofObject(ArgbEvaluatorCompat(), startColor, color).apply {
				addUpdateListener { view.imageTintList = ColorStateList.valueOf(it.animatedValue as Int) }
				start()
			}
		}
	}
}

@BindingAdapter("iconMaskColor")
fun setIconMaskColor(view: ImageView, colorResId: Int) {
	val color = view.context.getThemeColor(colorResId)
	view.colorFilter = getIconMask(color)
}
@BindingAdapter("saturation")
fun setSaturation(view: ImageView, value: Float) {
	val matrix = ColorMatrix().apply { setSaturation(value) }
	view.colorFilter = ColorMatrixColorFilter(matrix)
	if (value == 1f) {
		view.clearColorFilter()
	}
}

// Add an animation for alpha
@BindingAdapter("android:alpha", "animationDuration")
fun setAlpha(view: View, value: Float, duration: Int) {
	view.animation?.cancel()
	if (duration > 0) {
		ValueAnimator.ofFloat(view.alpha, value).apply {
			addUpdateListener { view.alpha = it.animatedValue as Float }
			this.duration = duration.toLong()
			start()
		}
	}
}

@BindingAdapter("animated")
fun setAnimated(view: ImageView, value: Boolean) {
	val drawable = view.drawable as? AnimatedVectorDrawable ?: return
	if (value) {
		drawable.start()
		drawable.registerAnimationCallback(object: Animatable2.AnimationCallback() {
			override fun onAnimationEnd(drawable: Drawable?) {
				if (view.isShown) {
					view.post { (drawable as? AnimatedVectorDrawable)?.start() }
				}
			}
		})
	} else {
		drawable.stop()
	}
}

// set an animator
val CANCELLABLE_ANIMATORS = WeakHashMap<View, Animator>()
@BindingAdapter("animator")
fun setAnimator(view: View, value: Animator?) {
	CANCELLABLE_ANIMATORS[view]?.cancel()
	if (value != null) {
		value.setTarget(view)
		value.start()
		CANCELLABLE_ANIMATORS[view] = value
	} else {
		view.animation?.cancel()
		view.clearAnimation()
	}
}

@BindingAdapter("onTouchDown")
fun setOnTouchDown(view: View, callback: View.OnClickListener) {
	view.setOnTouchListener { v, event ->
		if (event.actionMasked == MotionEvent.ACTION_DOWN) {
			callback.onClick(v)
		} else {
			v.performClick()
		}
		true
	}
}