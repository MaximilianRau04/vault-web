import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root',
})
export class NetworkStatusService {
  private lastShownAt = 0;
  private readonly cooldownMs = 5000;

  constructor(private toastr: ToastrService) {}

  showBackendUnavailable(): void {
    const now = Date.now();
    if (now - this.lastShownAt < this.cooldownMs) {
      return;
    }

    this.lastShownAt = now;
    this.toastr.error(
      'The server is currently unreachable. Please check your connection and try again.',
      'Server unavailable',
    );
  }
}
