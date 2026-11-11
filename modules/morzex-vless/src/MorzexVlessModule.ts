import { NativeModule, requireNativeModule } from 'expo';

export type VlessStatus = {
  available: boolean;
  connected: boolean;
  message: string;
};

export type VlessStartResult = {
  started: boolean;
  connected: boolean;
  message: string;
};

export type VlessValidation = {
  valid: boolean;
  message: string;
};

declare class MorzexVlessModule extends NativeModule {
  start(xrayJson: string): Promise<VlessStartResult>;
  getStatus(): VlessStatus;
  testConfig(xrayJson: string): Promise<VlessValidation>;
  stop(): Promise<{ stopping: boolean; message: string }>;
}

export default requireNativeModule<MorzexVlessModule>('MorzexVless');
