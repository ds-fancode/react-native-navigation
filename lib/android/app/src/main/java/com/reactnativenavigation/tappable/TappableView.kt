package com.reactnativenavigation.tappable

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.UIManagerModule
import com.facebook.react.views.view.ReactViewGroup
import com.reactnativenavigation.RNNTappableViewManager
import com.reactnativenavigation.react.NavigationModule
import java.util.ArrayList

@SuppressLint("ViewConstructor")
class TappableView(
        private val reactContext: ReactContext,
        private val navigationModule: NavigationModule
) : ReactViewGroup(reactContext) {

    val navigationFunctionMap = mapOf(
            "push" to navigationModule::push,
            "pop" to navigationModule::pop,
            "popToRoot" to navigationModule::popToRoot,
            "showModal" to navigationModule::showModal,
            "dismissModal" to navigationModule::dismissModal,
            "showOverlay" to navigationModule::showOverlay,
            "dismissOverlay" to navigationModule::dismissOverlay,
            "setStackRoot" to navigationModule::setStackRoot,
            "setRoot" to navigationModule::setRoot,
            "mergeOptions" to navigationModule::mergeOptions,
            "dismissAllModals" to navigationModule::dismissAllModals,
    )

    var navigatorType: String? = null
    var arguments: ReadableArray? = null
    var disabled = false
    var activeOpacity: Double = 0.8

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (disabled) return false
        if (event?.action == MotionEvent.ACTION_DOWN) {
            this.animate().alpha(this.activeOpacity.toFloat()).setDuration(200).start()
            return true
        } else if (event?.action == MotionEvent.ACTION_UP) {
            if (this.navigatorType != null && navigationFunctionMap.containsKey(this.navigatorType) && this.arguments != null) {
                val targetFunction = navigationFunctionMap[this.navigatorType]
                if (targetFunction != null) {
                    val reqArity = targetFunction.parameters.size
                    val availableArgs = toArrayList(this.arguments!!).toArray()
                    if (availableArgs.size + 1 == reqArity) {
                        val promise = createPromise(reactContext, this, event)
                        targetFunction.call(*availableArgs, promise)
                    } else if (availableArgs.size == reqArity)
                        targetFunction.call(*availableArgs)
                }
            }
            val params: WritableMap = Arguments.createMap()
            params.putDouble("locationX", event.x.toDouble())
            params.putDouble("locationY", event.y.toDouble())
            sendEvent(reactContext, RNNTappableViewManager.ON_CLICK, this, params)
        }
        this.animate().alpha(1F).setDuration(200).start()
        return false
    }

    private fun sendEvent(
            reactContext: ReactContext,
            eventName: String,
            view: View,
            params: WritableMap?
    ) {
        reactContext.getNativeModule(
                UIManagerModule::class.java
        )?.eventDispatcher?.dispatchEvent(TappableEvent(view.id, eventName, params))
    }

    fun createPromise(reactContext: ReactContext, view: View, event: MotionEvent): Promise {
        val promise: Promise = object : Promise {
            override fun resolve(value: Any?) {
                sendEvent(reactContext, RNNTappableViewManager.ON_RESOLVE, view,null)
            }
            override fun reject(code: String, message: String) {
                val params: WritableMap = Arguments.createMap()
                params.putString("message", message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(code: String, throwable: Throwable) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", throwable.message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(code: String, message: String, throwable: Throwable) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(throwable: Throwable) {
                val params: WritableMap = Arguments.createMap()
                params.putString("message", throwable.message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(throwable: Throwable, userInfo: WritableMap) {
                val params: WritableMap = Arguments.createMap()
                params.putString("message", throwable.message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(code: String, userInfo: WritableMap) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", code)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(code: String, throwable: Throwable, userInfo: WritableMap) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", throwable.message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(code: String, message: String, userInfo: WritableMap) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(
                    code: String,
                    message: String,
                    throwable: Throwable,
                    userInfo: WritableMap
            ) {
                val params: WritableMap = Arguments.createMap()
                params.putString("code", code)
                params.putString("message", message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
            override fun reject(message: String) {
                val params: WritableMap = Arguments.createMap()
                params.putString("message", message)
                sendEvent(reactContext, RNNTappableViewManager.ON_REJECT, view, params)
            }
        }
        return promise
    }

    private fun toArrayList(array: ReadableArray): ArrayList<Any?> {
        val arrayList = ArrayList<Any?>(array.size())
        var i = 0
        val size = array.size()
        while (i < size) {
            when (array.getType(i)) {
                ReadableType.Null -> arrayList.add(null)
                ReadableType.Boolean -> arrayList.add(array.getBoolean(i))
                ReadableType.Number -> arrayList.add(array.getDouble(i))
                ReadableType.String -> arrayList.add(array.getString(i))
                ReadableType.Map -> arrayList.add(array.getMap(i))
                ReadableType.Array -> arrayList.add(toArrayList(array.getArray(i)))
                else -> throw IllegalArgumentException("Could not convert object at index: $i.")
            }
            i++
        }
        return arrayList
    }
}
