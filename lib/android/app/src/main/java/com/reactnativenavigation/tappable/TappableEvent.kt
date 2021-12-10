package com.reactnativenavigation.tappable

import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event
import com.facebook.react.uimanager.events.RCTEventEmitter

/**
 * Event emitted from tappable
 */
class TappableEvent(viewId: Int, private val mEventName: String, private val mEventData: WritableMap?) :
        Event<TappableEvent>(viewId) {

    override fun getEventName(): String = mEventName

    override fun dispatch(rctEventEmitter: RCTEventEmitter) =
            rctEventEmitter.receiveEvent(viewTag, eventName, mEventData)

}
