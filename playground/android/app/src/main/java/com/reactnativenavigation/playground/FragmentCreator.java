package com.reactnativenavigation.playground;

import com.facebook.react.ReactInstanceManager;
import com.reactnativenavigation.options.Options;
import com.reactnativenavigation.viewcontrollers.externalcomponent.ExternalComponent;
import com.reactnativenavigation.viewcontrollers.externalcomponent.ExternalComponentCreator;

import org.json.JSONObject;

import androidx.fragment.app.FragmentActivity;

public class FragmentCreator implements ExternalComponentCreator {

    @Override
    public ExternalComponent create(FragmentActivity activity, ReactInstanceManager reactInstanceManager, String componentId, JSONObject props, Options layoutOptions) {
        return new FragmentComponent(activity, props);
    }
}
