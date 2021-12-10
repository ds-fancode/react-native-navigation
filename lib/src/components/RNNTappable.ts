import { CommandName } from '../interfaces/CommandName';
import { Layout } from '../interfaces/Layout';
import { Options } from '../interfaces/Options';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  HostComponent,
  NativeTouchEvent,
  ViewProps,
} from 'react-native';

const IS_IOS = Platform.OS === 'ios';

const LINKING_ERROR =
  `The package '@dream11mobile/react-native-navigation' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n';

export const TappableSupportedCommands = [
  CommandName.Push,
  CommandName.Pop,
  CommandName.PopToRoot,
  CommandName.ShowModal,
  CommandName.DismissModal,
  CommandName.ShowOverlay,
  CommandName.DismissOverlay,
  CommandName.SetStackRoot,
  CommandName.SetRoot,
  CommandName.DismissAllModals,
];

type TappableNavigationProp = {
  navigatorType: typeof TappableSupportedCommands[0];
  arguments: (String | Layout | Options)[];
  disabled?: boolean;
  activeOpacity?: number;
  onClick?: (event: NativeTouchEvent) => void;
  onResolve?: (data: any) => void;
  onReject?: (error: { message?: string; code?: string }) => void;
};

type FcReactNativeTappableProps = ViewProps & TappableNavigationProp;

const ComponentName = 'RNNTappableView';

export const RNNTappableView: HostComponent<FcReactNativeTappableProps> = IS_IOS
  ? null
  : UIManager.getViewManagerConfig(ComponentName) != null
  ? requireNativeComponent<FcReactNativeTappableProps>(ComponentName)
  : ((() => {
      throw new Error(LINKING_ERROR);
    }) as any);
