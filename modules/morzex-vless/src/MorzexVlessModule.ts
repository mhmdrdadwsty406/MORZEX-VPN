import { NativeModule, requireNativeModule } from 'expo';

type VlessStatus = {
  available: boolean;
  connected: boolean;
  message: string;
};

declare class MorzexVlessModule extends NativeModule {
  getStatus(): VlessStatus;
}

export default requireNativeModule<MorzexVlessModule>('MorzexVless');
