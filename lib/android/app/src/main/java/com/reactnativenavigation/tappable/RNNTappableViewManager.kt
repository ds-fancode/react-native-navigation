package com.reactnativenavigation

import com.facebook.react.ReactNativeHost
import com.facebook.react.bridge.*
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.reactnativenavigation.react.NavigationModule
import com.reactnativenavigation.tappable.TappableView
import kotlin.collections.HashMap
import kotlin.collections.set

class RNNTappableViewManager(private val navigationModule: NavigationModule, private val reactNativeHost: ReactNativeHost) : ViewGroupManager<TappableView>() {
  companion object {
    val ON_CLICK = "onClick"
    val ON_RESOLVE = "onResolve"
    val ON_REJECT = "onReject"
  }

  override fun getName() = "RNNTappableView"

  override fun createViewInstance(reactContext: ThemedReactContext): TappableView {
    return TappableView(reactContext, navigationModule)
  }

  @ReactProp(name = "navigatorType")
  fun setNavigatorType(view: TappableView, navigatorType: String) {
    view.navigatorType = navigatorType
  }

  @ReactProp(name = "arguments")
  fun setArguments(view: TappableView, arguments: ReadableArray) {
    view.arguments = arguments
  }

  @ReactProp(name = "activeOpacity")
  fun setArguments(view: TappableView, activeOpacity: Double) {
    view.activeOpacity = activeOpacity
  }

  @ReactProp(name = "disabled")
  fun setArguments(view: TappableView, disabled: Boolean) {
    view.disabled = disabled
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
    val map: MutableMap<String, Any> = HashMap()
    map[ON_CLICK] = MapBuilder.of("registrationName", ON_CLICK)
    map[ON_RESOLVE] = MapBuilder.of("registrationName", ON_RESOLVE)
    map[ON_REJECT] = MapBuilder.of("registrationName", ON_REJECT)
    return map
  }
}
