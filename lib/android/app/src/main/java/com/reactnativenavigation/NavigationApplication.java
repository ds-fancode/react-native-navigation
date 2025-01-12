package com.reactnativenavigation;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.soloader.SoLoader;
import com.reactnativenavigation.react.ReactGateway;
import com.reactnativenavigation.utils.DefaultLogger;
import com.reactnativenavigation.utils.ILogger;
import com.reactnativenavigation.viewcontrollers.externalcomponent.ExternalComponentCreator;

import java.util.HashMap;
import java.util.Map;

public abstract class NavigationApplication extends Application implements ReactApplication {

    private ReactGateway reactGateway;
    public static NavigationApplication instance;
    private static ILogger DEFAULT_LOGGER;
    final Map<String, ExternalComponentCreator> externalComponents = new HashMap<>();

    protected boolean navigatingToAnotherActivity = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        DEFAULT_LOGGER = new DefaultLogger(Log.INFO);
        instance = this;
        SoLoader.init(this, false);
        reactGateway = createReactGateway();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        navigatingToAnotherActivity=true;
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        super.startActivity(intent, options);
        navigatingToAnotherActivity=true;
    }

    /**
     * Subclasses of NavigationApplication may override this method to create the singleton instance
     * of {@link ReactGateway}. For example, subclasses may wish to provide a custom {@link ReactNativeHost}
     * with the ReactGateway. This method will be called exactly once, in the application's {@link #onCreate()} method.
     * <p>
     * Custom {@link ReactNativeHost}s must be sure to include {@link com.reactnativenavigation.react.NavigationPackage}
     *
     * @return a singleton {@link ReactGateway}
     */
    protected ReactGateway createReactGateway() {
        return new ReactGateway(getReactNativeHost());
    }

    public ReactGateway getReactGateway() {
        return reactGateway;
    }

    /**
     * Generally no need to override this; override for custom permission handling.
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    /**
     * Register a native View which can be displayed using the given {@code name}
     *
     * @param name    Unique name used to register the native view
     * @param creator Used to create the view at runtime
     */
    @SuppressWarnings("unused")
    public void registerExternalComponent(String name, ExternalComponentCreator creator) {
        if (externalComponents.containsKey(name)) {
            throw new RuntimeException("A component has already been registered with this name: " + name);
        }
        externalComponents.put(name, creator);
    }

    public final Map<String, ExternalComponentCreator> getExternalComponents() {
        return externalComponents;
    }

    public static ILogger getLogger() {
        return DEFAULT_LOGGER;
    }
}
