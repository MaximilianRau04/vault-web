import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  AfterViewChecked,
  OnDestroy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatMessageDto } from '../../models/dtos/ChatMessageDto';
import { WebSocketService } from '../../services/web-socket.service';
import { PrivateChatService } from '../../services/private-chat.service';
import { Subscription } from 'rxjs/internal/Subscription';
import { E2eeService } from '../../services/e2ee.service';
import { DeviceDto } from '../../models/dtos/DeviceDto';

interface ChatMessageView {
  content: string;
  senderUsername?: string;
  privateChatId?: number;
  timestamp: string;
}

@Component({
  selector: 'app-private-chat-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './private-chat-dialog.component.html',
  styleUrls: ['./private-chat-dialog.component.scss'],
})
export class PrivateChatDialogComponent
  implements OnInit, OnDestroy, AfterViewChecked
{
  @Input() username!: string;
  @Input() currentUsername!: string | null;
  @Input() privateChatId!: number;
  @Output() closeChat = new EventEmitter<void>();

  messages: ChatMessageView[] = [];
  newMessage = '';
  private devices: DeviceDto[] = [];

  @ViewChild('messageContainer') messageContainer!: ElementRef<HTMLDivElement>;
  private privateMessageSub!: Subscription;

  private shouldScroll = false;

  constructor(
    private wsService: WebSocketService,
    private chatService: PrivateChatService,
    private e2eeService: E2eeService,
  ) {}

  ngOnInit(): void {
    this.e2eeService
      .ensureDeviceRegistered()
      .then(() => {
        this.refreshDevices();

        this.chatService.getMessages(this.privateChatId).subscribe({
          next: (msgs) => {
            this.decryptMessages(msgs);
          },
          error: () => {
            console.error('Error loading messages for private chat');
          },
        });

        this.privateMessageSub = this.wsService
          .subscribeToPrivateMessages()
          .subscribe((msg) => {
            if (msg.privateChatId === this.privateChatId) {
              this.decryptAndAppendMessage(msg);
            }
          });
      })
      .catch(() => {
        console.error('Failed to initialize end-to-end encryption');
      });
  }

  ngOnDestroy(): void {
    this.privateMessageSub?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  scrollToBottom(): void {
    try {
      this.messageContainer.nativeElement.scrollTop =
        this.messageContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error('Scroll to bottom failed:', err);
    }
  }

  sendMessage(): void {
    if (!this.newMessage.trim()) return;

    this.sendEncryptedMessage(this.newMessage);
  }

  onClose(): void {
    this.closeChat.emit();
  }

  private decryptMessages(messages: ChatMessageDto[]): void {
    Promise.all(messages.map(async (msg) => this.toViewMessage(msg))).then(
      (viewMessages) => {
        this.messages = viewMessages.filter(
          (msg): msg is ChatMessageView => msg !== null,
        );
        this.shouldScroll = true;
      },
    );
  }

  private decryptAndAppendMessage(message: ChatMessageDto): void {
    this.toViewMessage(message).then((viewMessage) => {
      if (!viewMessage) {
        return;
      }
      this.messages.push(viewMessage);
      this.shouldScroll = true;
    });
  }

  private async toViewMessage(
    message: ChatMessageDto,
  ): Promise<ChatMessageView | null> {
    let content = message.content ?? null;
    if (message.e2eePayload) {
      content = await this.e2eeService.decryptPayload(message.e2eePayload);
    }
    if (!content) {
      content = 'Encrypted message';
    }
    return {
      content,
      senderUsername: message.senderUsername,
      privateChatId: message.privateChatId,
      timestamp: message.timestamp,
    };
  }

  private async sendEncryptedMessage(plaintext: string): Promise<void> {
    await this.e2eeService.ensureDeviceRegistered();
    this.devices = await this.fetchDevices();

    if (!this.devices.length) {
      console.error('No devices available for encryption');
      return;
    }

    const payload = await this.e2eeService.encryptForDevices(
      plaintext,
      this.devices,
    );

    const message: ChatMessageDto = {
      timestamp: new Date().toISOString(),
      senderUsername: this.currentUsername ? this.currentUsername : 'Unknown',
      privateChatId: this.privateChatId,
      senderDeviceId: payload.senderDeviceId,
      e2eePayload: JSON.stringify(payload),
    };

    this.wsService.sendPrivateMessage(message);
    this.newMessage = '';
  }

  private refreshDevices(): void {
    this.chatService.getDevices(this.privateChatId).subscribe({
      next: (devices) => {
        this.devices = devices;
      },
      error: () => {
        console.error('Error loading devices for private chat');
      },
    });
  }

  private fetchDevices(): Promise<DeviceDto[]> {
    return new Promise<DeviceDto[]>((resolve) => {
      this.chatService.getDevices(this.privateChatId).subscribe({
        next: (devices) => resolve(devices),
        error: () => resolve([]),
      });
    });
  }
}
