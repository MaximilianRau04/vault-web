import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { DeviceDto } from '../models/dtos/DeviceDto';
import { AuthService } from './auth.service';
import { E2eeService } from './e2ee.service';

describe('E2eeService', () => {
  const DEVICE_ID_KEY = 'vault.web.deviceId';
  const DEVICE_KEYPAIR_KEY = 'vault.web.deviceKeyPair';

  let service: E2eeService;
  let authMock: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    localStorage.clear();

    authMock = jasmine.createSpyObj<AuthService>('AuthService', [
      'getUsername',
      'getToken',
      'refresh',
      'saveToken',
    ]);
    authMock.getUsername.and.returnValue(null);
    authMock.getToken.and.returnValue(null);
    authMock.refresh.and.returnValue(of({ token: 'token' }));

    service = new E2eeService({} as HttpClient, authMock);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should encrypt and decrypt a real plaintext roundtrip for recipient device', async () => {
    const sender = await generateIdentity();
    const recipient = await generateIdentity();

    localStorage.setItem(DEVICE_ID_KEY, sender.deviceId);
    localStorage.setItem(
      DEVICE_KEYPAIR_KEY,
      JSON.stringify({
        publicKey: sender.publicKey,
        privateKey: sender.privateKey,
      }),
    );

    const devices: DeviceDto[] = [
      {
        deviceId: recipient.deviceId,
        publicKey: JSON.stringify(recipient.publicKey),
        userId: 2,
        username: 'recipient',
      },
    ];

    const plaintext = 'Hello E2EE Roundtrip äöü 123';
    const payload = await service.encryptForDevices(plaintext, devices);

    localStorage.setItem(DEVICE_ID_KEY, recipient.deviceId);
    localStorage.setItem(
      DEVICE_KEYPAIR_KEY,
      JSON.stringify({
        publicKey: recipient.publicKey,
        privateKey: recipient.privateKey,
      }),
    );

    const decrypted = await service.decryptPayload(JSON.stringify(payload));
    expect(decrypted).toBe(plaintext);
  });

  it('should return null when payload has no entry for current device', async () => {
    const sender = await generateIdentity();
    const recipient = await generateIdentity();
    const otherRecipient = await generateIdentity();

    localStorage.setItem(DEVICE_ID_KEY, sender.deviceId);
    localStorage.setItem(
      DEVICE_KEYPAIR_KEY,
      JSON.stringify({
        publicKey: sender.publicKey,
        privateKey: sender.privateKey,
      }),
    );

    const devices: DeviceDto[] = [
      {
        deviceId: otherRecipient.deviceId,
        publicKey: JSON.stringify(otherRecipient.publicKey),
        userId: 3,
        username: 'other',
      },
    ];

    const payload = await service.encryptForDevices('Secret', devices);

    localStorage.setItem(DEVICE_ID_KEY, recipient.deviceId);
    localStorage.setItem(
      DEVICE_KEYPAIR_KEY,
      JSON.stringify({
        publicKey: recipient.publicKey,
        privateKey: recipient.privateKey,
      }),
    );

    const decrypted = await service.decryptPayload(JSON.stringify(payload));
    expect(decrypted).toBeNull();
  });
});

async function generateIdentity(): Promise<{
  deviceId: string;
  publicKey: JsonWebKey;
  privateKey: JsonWebKey;
}> {
  const keyPair = await crypto.subtle.generateKey(
    { name: 'ECDH', namedCurve: 'P-256' },
    true,
    ['deriveBits', 'deriveKey'],
  );
  const publicKey = await crypto.subtle.exportKey('jwk', keyPair.publicKey);
  const privateKey = await crypto.subtle.exportKey('jwk', keyPair.privateKey);
  return {
    deviceId: crypto.randomUUID(),
    publicKey,
    privateKey,
  };
}
