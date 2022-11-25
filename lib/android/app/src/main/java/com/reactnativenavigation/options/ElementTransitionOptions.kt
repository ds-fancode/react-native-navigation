package com.reactnativenavigation.options

import android.animation.Animator
<<<<<<< HEAD
import android.animation.AnimatorSet
import android.util.Property
=======
>>>>>>> d936545ffdce2eb4e671aeca6f390c767517e4c2
import android.view.View
import org.json.JSONObject

class ElementTransitionOptions(json: JSONObject) {
    private val animation: AnimationOptions = AnimationOptions(json)
    val id: String
        get() = animation.id.get()

    fun getAnimation(view: View): Animator = animation.getAnimation(view)

    fun setValueDy(property: Property<View?, Float?>?, fromDelta: Float, toDelta: Float) {
        animation.setValueDy(property, fromDelta, toDelta)
    }
}