import { Injectable } from '@angular/core';
import { UiToastService } from './ui-toast.service';

@Injectable({
  providedIn: 'root',
})
export class NetworkStatusService {
  private lastShownAt = 0;
  private readonly cooldownMs = 5000;

  constructor(private toast: UiToastService) {}

  showBackendUnavailable(): void {
    const now = Date.now();
    if (now - this.lastShownAt < this.cooldownMs) {
      return;
    }

    this.lastShownAt = now;
    this.toast.error(
      'Server unavailable',
      'The server is currently unreachable. Please check your connection.',
    );
  }
}
