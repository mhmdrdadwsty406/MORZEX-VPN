import { NativeModule, requireNativeModule } from 'expo';

declare class MorzexWireguardModule extends NativeModule<{}> {}

export default requireNativeModule<MorzexWireguardModule>('MorzexWireguard');
