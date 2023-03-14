package com.reactnativenavigation.react;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.reactnativenavigation.NavigationActivity;
import com.reactnativenavigation.NavigationApplication;
import com.reactnativenavigation.options.LayoutFactory;
import com.reactnativenavigation.options.LayoutNode;
import com.reactnativenavigation.options.Options;
import com.reactnativenavigation.options.parsers.JSONParser;
import com.reactnativenavigation.options.parsers.LayoutNodeParser;
import com.reactnativenavigation.options.parsers.TypefaceLoader;
import com.reactnativenavigation.react.events.EventEmitter;
import com.reactnativenavigation.utils.LaunchArgsParser;
import com.reactnativenavigation.utils.Now;
import com.reactnativenavigation.utils.SystemUiUtils;
import com.reactnativenavigation.utils.UiThread;
import com.reactnativenavigation.utils.UiUtils;
import com.reactnativenavigation.viewcontrollers.navigator.Navigator;
import com.reactnativenavigation.viewcontrollers.viewcontroller.ViewController;

import java.util.ArrayList;
import java.util.Objects;

import static com.reactnativenavigation.utils.UiUtils.pxToDp;

import android.app.Activity;

@ReactModule(name = "RNNBridgeModule")
public class NavigationModule extends ReactContextBaseJavaModule {
    public static final String NAME = "RNNBridgeModule";

    private final Now now = new Now();
    public static ReactInstanceManager reactInstanceManager;
    public static Navigator currentNavigator;
    private final JSONParser jsonParser;
    private final LayoutFactory layoutFactory;
    private EventEmitter eventEmitter;

    @SuppressWarnings("WeakerAccess")
    public NavigationModule(ReactApplicationContext reactContext, ReactInstanceManager reactInstanceManager, LayoutFactory layoutFactory) {
        this(reactContext, reactInstanceManager, new JSONParser(), layoutFactory);
    }

    public NavigationModule(ReactApplicationContext reactContext, ReactInstanceManager reactInstanceManager, JSONParser jsonParser, LayoutFactory layoutFactory) {
        super(reactContext);
        this.reactInstanceManager = reactInstanceManager;
        this.jsonParser = jsonParser;
        this.layoutFactory = layoutFactory;
        reactContext.addLifecycleEventListener(listenerAdapter);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void getLaunchArgs(String commandId, Promise promise) {
        promise.resolve(LaunchArgsParser.parse(activity()));
    }

    private WritableMap createNavigationConstantsMap() {
        ReactApplicationContext ctx = getReactApplicationContext();
        final Activity currentActivity = ctx.getCurrentActivity();
        WritableMap constants = Arguments.createMap();
        constants.putString(Constants.BACK_BUTTON_JS_KEY, Constants.BACK_BUTTON_ID);
        constants.putDouble(Constants.BOTTOM_TABS_HEIGHT_KEY, pxToDp(ctx, UiUtils.getBottomTabsHeight(ctx)));
        constants.putDouble(Constants.STATUS_BAR_HEIGHT_KEY, pxToDp(ctx, SystemUiUtils.getStatusBarHeight(currentActivity)));
        constants.putDouble(Constants.TOP_BAR_HEIGHT_KEY, pxToDp(ctx, UiUtils.getTopBarHeight(ctx)));
        return constants;
    }

    @ReactMethod
    public void getNavigationConstants(Promise promise) {
        promise.resolve(createNavigationConstantsMap());
    }

    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap getNavigationConstantsSync() {
        return createNavigationConstantsMap();
    }

    @ReactMethod
    public void setRoot(String commandId, ReadableMap rawLayoutTree, Promise promise) {
        final LayoutNode layoutTree = LayoutNodeParser.parse(Objects.requireNonNull(jsonParser.parse(rawLayoutTree).optJSONObject("root")));
        handle(() -> {
            final ViewController<?> viewController = layoutFactory.create(layoutTree);
            navigator().setRoot(viewController, new NativeCommandListener("setRoot", commandId, promise, eventEmitter, now), reactInstanceManager);
        });
    }

    @ReactMethod
    public void setDefaultOptions(ReadableMap options) {
        handle(() -> {
            Options defaultOptions = parse(options);
            layoutFactory.setDefaultOptions(defaultOptions);
            navigator().setDefaultOptions(defaultOptions);
        });
    }

    @ReactMethod
    public void mergeOptions(String onComponentId, @Nullable ReadableMap options) {
        handle(() -> navigator().mergeOptions(onComponentId, parse(options)));
    }

    @ReactMethod
    public void push(String commandId, String onComponentId, ReadableMap rawLayoutTree, Promise promise) {
        final LayoutNode layoutTree = LayoutNodeParser.parse(jsonParser.parse(rawLayoutTree));
        handle(() -> {
            final ViewController<?> viewController = layoutFactory.create(layoutTree);
            navigator().push(onComponentId, viewController, new NativeCommandListener("push", commandId, promise, eventEmitter, now));
        });
    }

    public void push(String onComponentId, LayoutNode layoutTree, CommandListener listener) {
        handle(() -> {
            final ViewController viewController = layoutFactory.create(layoutTree);
            navigator().push(onComponentId, viewController, listener);
        });
    }

    @ReactMethod
    public void switchToPIP(String commandId, String componentId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> navigator().switchToPIP(componentId, parse(mergeOptions), new NativeCommandListener("switchToPIP", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void pushAsPIP(String commandId, String onComponentId, ReadableMap rawLayoutTree, Promise promise) {
        final LayoutNode layoutTree = LayoutNodeParser.parse(jsonParser.parse(rawLayoutTree));
        handle(() -> {
            final ViewController viewController = layoutFactory.create(layoutTree);
            navigator().pushAsPIP(onComponentId, viewController, new NativeCommandListener("pushAsPIP", commandId, promise, eventEmitter, now));
        });
    }

    @ReactMethod
    public void closePIP(String commandId, Promise promise) {
        handle(() -> navigator().closePIP(new NativeCommandListener("closePIP", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void restorePIP(String commandId, String stackId, Promise promise) {
        handle(() -> navigator().restorePIP(stackId, new NativeCommandListener("restorePIP", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void setStackRoot(String commandId, String onComponentId, ReadableArray children, Promise promise) {
        handle(() -> {
            ArrayList<ViewController<?>> _children = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                final LayoutNode layoutTree = LayoutNodeParser.parse(jsonParser.parse(children.getMap(i)));
                _children.add(layoutFactory.create(layoutTree));
            }
            navigator().setStackRoot(onComponentId, _children, new NativeCommandListener("setStackRoot", commandId, promise, eventEmitter, now));
        });
    }

    @ReactMethod
    public void pop(String commandId, String componentId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> navigator().pop(componentId, parse(mergeOptions), new NativeCommandListener("pop", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void popTo(String commandId, String componentId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> navigator().popTo(componentId, parse(mergeOptions), new NativeCommandListener("popTo", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void popToRoot(String commandId, String componentId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> navigator().popToRoot(componentId, parse(mergeOptions), new NativeCommandListener("popToRoot", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void showModal(String commandId, ReadableMap rawLayoutTree, Promise promise) {
        final LayoutNode layoutTree = LayoutNodeParser.parse(jsonParser.parse(rawLayoutTree));
        handle(() -> {
            final ViewController<?> viewController = layoutFactory.create(layoutTree);
            navigator().showModal(viewController, new NativeCommandListener("showModal", commandId, promise, eventEmitter, now));
        });
    }

    @ReactMethod
    public void dismissModal(String commandId, String componentId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> {
            navigator().mergeOptions(componentId, parse(mergeOptions));
            navigator().dismissModal(componentId, new NativeCommandListener("dismissModal", commandId, promise, eventEmitter, now));
        });
    }

    @ReactMethod
    public void dismissAllModals(String commandId, @Nullable ReadableMap mergeOptions, Promise promise) {
        handle(() -> navigator().dismissAllModals(parse(mergeOptions), new NativeCommandListener("dismissAllModals", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void showOverlay(String commandId, ReadableMap rawLayoutTree, Promise promise) {
        final LayoutNode layoutTree = LayoutNodeParser.parse(jsonParser.parse(rawLayoutTree));
        handle(() -> {
            final ViewController<?> viewController = layoutFactory.create(layoutTree);
            navigator().showOverlay(viewController, new NativeCommandListener("showOverlay", commandId, promise, eventEmitter, now));
        });
    }

    @ReactMethod
    public void dismissOverlay(String commandId, String componentId, Promise promise) {
        handle(() -> navigator().dismissOverlay(componentId, new NativeCommandListener("dismissOverlay", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void dismissAllOverlays(String commandId, Promise promise) {
        handle(() -> navigator().dismissAllOverlays(new NativeCommandListener("dismissAllOverlays", commandId, promise, eventEmitter, now)));
    }

    @ReactMethod
    public void setPIPHostId(String componentId) {
        handle(() -> navigator().setPIPHostId(componentId));
    }

    private Navigator navigator() {
        return activity().getNavigator();
    }

    private Options parse(@Nullable ReadableMap mergeOptions) {
        ReactApplicationContext ctx = getReactApplicationContext();
        return mergeOptions ==
                null ? Options.EMPTY : Options.parse(ctx, new TypefaceLoader(activity()), jsonParser.parse(mergeOptions));
    }

    protected void handle(Runnable task) {
        UiThread.post(() -> {
            if (getCurrentActivity() != null && !activity().isFinishing()) {
                currentNavigator = activity().getNavigator();
                task.run();
            }
        });
    }

    protected NavigationActivity activity() {
        return (NavigationActivity) getCurrentActivity();
    }

    @Override
    public void onCatalystInstanceDestroy() {
        final NavigationActivity navigationActivity = activity();
        if (navigationActivity != null) {
            navigationActivity.onCatalystInstanceDestroy();
        }
        super.onCatalystInstanceDestroy();
    }

    private LifecycleEventListenerAdapter listenerAdapter = new LifecycleEventListenerAdapter() {
        @Override
        public void onHostPause() {
            super.onHostPause();
            UiUtils.runOnMainThread(() -> {
                try {
                    navigator().onHostPause();
                } catch (Exception e) {

                }
            });
        }
        
        @Override
        public void onHostResume() {
            eventEmitter = new EventEmitter(getReactApplicationContext());
            navigator().setEventEmitter(eventEmitter);
            layoutFactory.init(
                    activity(),
                    eventEmitter,
                    navigator().getChildRegistry(),
                    ((NavigationApplication) activity().getApplication()).getExternalComponents()
            );
            UiUtils.runOnMainThread(() -> navigator().onHostResume());
        }
    };
}
