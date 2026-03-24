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
  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;
  private privateMessageSub!: Subscription;

  private shouldScroll = false;
  isSearchOpen = false;
  searchQuery = '';
  filteredMessages: ChatMessageView[] = [];

  constructor(
    private wsService: WebSocketService,
    private chatService: PrivateChatService,
    private e2eeService: E2eeService,
  ) {}

  ngOnInit(): void {
    this.chatService.getMessages(this.privateChatId).subscribe({
      next: (msgs) => {
        this.decryptMessages(msgs);
        this.shouldScroll = true;
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

    void this.initializeE2ee();
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

    void this.sendEncryptedMessage(this.newMessage);
  }

  onClose(): void {
    this.closeChat.emit();
  }

  private async initializeE2ee(): Promise<void> {
    try {
      await this.e2eeService.ensureDeviceRegistered();
      await this.refreshDevices();
    } catch {
      console.error('Failed to initialize end-to-end encryption');
    }
  }

  private decryptMessages(messages: ChatMessageDto[]): void {
    Promise.all(
      messages.map(async (msg, index) => {
        try {
          return await this.toViewMessage(msg);
        } catch (err) {
          console.error(
            'Failed to decrypt message in private chat',
            {
              privateChatId: this.privateChatId,
              messageIndex: index,
            },
            err,
          );
          return null;
        }
      }),
    )
      .then((viewMessages) => {
        const successfulMessages = viewMessages.filter(
          (msg): msg is ChatMessageView => msg !== null,
        );
        if (successfulMessages.length !== viewMessages.length) {
          console.warn('Some messages failed to decrypt for private chat', {
            privateChatId: this.privateChatId,
            totalMessages: viewMessages.length,
            decryptedMessages: successfulMessages.length,
          });
        }
        this.messages = successfulMessages;
        this.applySearch();
        this.shouldScroll = true;
      })
      .catch((err) => {
        console.error(
          'Failed to decrypt one or more messages for private chat',
          this.privateChatId,
          err,
        );
      });
  }

  private decryptAndAppendMessage(message: ChatMessageDto): void {
    this.toViewMessage(message)
      .then((viewMessage) => {
        if (!viewMessage) {
          console.warn(
            'Failed to decrypt incoming message for private chat',
            this.privateChatId,
          );
          return;
        }
        this.messages.push(viewMessage);
        this.applySearch();
        this.shouldScroll = !this.searchQuery.trim();
      })
      .catch((err) => {
        console.error(
          'Error decrypting incoming message for private chat',
          this.privateChatId,
          err,
        );
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
      content = message.e2eePayload
        ? 'Unable to decrypt message'
        : 'Encrypted message';
    }
    return {
      content,
      senderUsername: message.senderUsername,
      privateChatId: message.privateChatId,
      timestamp: message.timestamp,
    };
  }

  private async sendEncryptedMessage(plaintext: string): Promise<void> {
    try {
      await this.e2eeService.ensureDeviceRegistered();
      this.devices = await this.fetchDevices(true);

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
    } catch (error) {
      console.error('Failed to send encrypted message', error);
    }
  }

  private async refreshDevices(): Promise<void> {
    this.devices = await this.fetchDevices(true);
  }

  private fetchDevices(forceRefresh = false): Promise<DeviceDto[]> {
    if (!forceRefresh && this.devices.length) {
      return Promise.resolve(this.devices);
    }
    return new Promise<DeviceDto[]>((resolve) => {
      this.chatService.getDevices(this.privateChatId).subscribe({
        next: (devices) => resolve(devices),
        error: (error) => {
          console.error('Error loading devices for private chat', error);
          resolve([]);
        },
      });
    });
  }

  toggleSearch() {
    this.isSearchOpen = !this.isSearchOpen;
    if (this.isSearchOpen) {
      setTimeout(() => this.searchInput?.nativeElement.focus(), 0);
    } else {
      this.searchQuery = '';
      this.applySearch(); // reset filteredMessages to all messages
      this.shouldScroll = true;
    }
  }

  applySearch(): void {
    const query = this.searchQuery.trim().toLowerCase();

    if (!query) {
      this.filteredMessages = [...this.messages];
      return;
    }

    this.filteredMessages = this.messages.filter((msg) =>
      msg.content.toLowerCase().includes(query),
    );
  }
}
