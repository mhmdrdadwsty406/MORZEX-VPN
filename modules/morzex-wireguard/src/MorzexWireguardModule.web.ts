import { registerWebModule, NativeModule } from 'expo';

class MorzexWireguardModule extends NativeModule<{}> {}

export default registerWebModule(MorzexWireguardModule, 'MorzexWireguardModule');
