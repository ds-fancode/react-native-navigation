package com.reactnativenavigation.react;

import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.reactnativenavigation.RNNTappableViewManager;
import com.reactnativenavigation.options.LayoutFactory;
import java.util.List;

import androidx.annotation.NonNull;

import static java.util.Collections.singletonList;

public class NavigationPackage implements ReactPackage {

    private ReactNativeHost reactNativeHost;

    private NavigationModule navigationModule;

    public NavigationPackage(final ReactNativeHost reactNativeHost) {
        this.reactNativeHost = reactNativeHost;
    }

    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        navigationModule = new NavigationModule(
                reactContext,
                reactNativeHost.getReactInstanceManager(),
                new LayoutFactory(reactNativeHost.getReactInstanceManager())
        );
        return singletonList(navigationModule);
    }

    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return singletonList(new RNNTappableViewManager(navigationModule, reactNativeHost));
    }
}
