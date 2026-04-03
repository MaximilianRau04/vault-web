import { Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

@Injectable({
  providedIn: 'root',
})
export class UiToastService {
  private readonly key = 'app-toast';

  constructor(private messageService: MessageService) {}

  success(summary: string, detail?: string): void {
    this.messageService.add({
      key: this.key,
      severity: 'success',
      summary,
      detail,
      life: 2800,
    });
  }

  info(summary: string, detail?: string): void {
    this.messageService.add({
      key: this.key,
      severity: 'info',
      summary,
      detail,
      life: 2800,
    });
  }

  warn(summary: string, detail?: string): void {
    this.messageService.add({
      key: this.key,
      severity: 'warn',
      summary,
      detail,
      life: 3400,
    });
  }

  error(summary: string, detail?: string): void {
    this.messageService.add({
      key: this.key,
      severity: 'error',
      summary,
      detail,
      life: 4500,
    });
  }
}
