import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  QueryList,
  ViewChildren,
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

interface EncryptedMessageBodyV1 {
  v: 1;
  text: string;
  clientTimestamp?: string;
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
  private lastDevicesRefreshAt = 0;
  private readonly devicesCacheTtlMs = 15000;

  @ViewChild('messageContainer') messageContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;
  @ViewChildren('messageBubble') messageBubbles!: QueryList<
    ElementRef<HTMLDivElement>
  >;
  private privateMessageSub!: Subscription;

  private shouldScroll = false;
  isSearchOpen = false;
  searchQuery = '';
  matchedMessageIndexes: number[] = [];
  private matchedMessageIndexSet = new Set<number>();
  activeMatchPosition = -1;

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
    if (this.isDuplicateMessage(message)) {
      return;
    }
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
    let timestamp = message.timestamp;
    if (message.e2eePayload) {
      const decrypted = await this.e2eeService.decryptPayload(
        message.e2eePayload,
      );
      const parsed = this.parseDecryptedMessageBody(decrypted);
      content = parsed.text;
      if (parsed.clientTimestamp) {
        timestamp = parsed.clientTimestamp;
      }
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
      timestamp,
    };
  }

  private async sendEncryptedMessage(plaintext: string): Promise<void> {
    try {
      await this.e2eeService.ensureDeviceRegistered();
      this.devices = await this.fetchDevices();

      if (!this.devices.length) {
        // One forced refresh before failing keeps the common path fast while handling
        // participant-device changes reliably.
        this.devices = await this.fetchDevices(true);
        if (!this.devices.length) {
          console.error('No devices available for encryption');
          return;
        }
      }

      const clientTimestamp = new Date().toISOString();
      const encryptedBody: EncryptedMessageBodyV1 = {
        v: 1,
        text: plaintext,
        clientTimestamp,
      };
      const payload = await this.e2eeService.encryptForDevices(
        JSON.stringify(encryptedBody),
        this.devices,
      );

      const message: ChatMessageDto = {
        timestamp: clientTimestamp,
        senderUsername: this.currentUsername ? this.currentUsername : 'Unknown',
        privateChatId: this.privateChatId,
        senderDeviceId: payload.senderDeviceId,
        e2eePayload: JSON.stringify(payload),
      };

      const isConnected = await this.wsService.ensureConnected();
      if (!isConnected) {
        console.error('WebSocket not connected. Message not sent.');
        return;
      }

      const sent = this.wsService.sendPrivateMessage(message);
      if (!sent) {
        console.error('WebSocket not connected. Message not sent.');
        return;
      }
      this.decryptAndAppendMessage(message);
      this.newMessage = '';
    } catch (error) {
      console.error('Failed to send encrypted message', error);
    }
  }

  private async refreshDevices(): Promise<void> {
    this.devices = await this.fetchDevices(true);
  }

  private fetchDevices(forceRefresh = false): Promise<DeviceDto[]> {
    const isCacheFresh =
      Date.now() - this.lastDevicesRefreshAt < this.devicesCacheTtlMs;
    if (!forceRefresh && this.devices.length && isCacheFresh) {
      return Promise.resolve(this.devices);
    }
    return new Promise<DeviceDto[]>((resolve) => {
      this.chatService.getDevices(this.privateChatId).subscribe({
        next: (devices) => {
          this.lastDevicesRefreshAt = Date.now();
          resolve(devices);
        },
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
      this.applySearch();
      this.shouldScroll = true;
    }
  }

  applySearch(): void {
    const query = this.searchQuery.trim().toLowerCase();

    if (!query) {
      this.matchedMessageIndexes = [];
      this.matchedMessageIndexSet.clear();
      this.activeMatchPosition = -1;
      return;
    }

    this.matchedMessageIndexes = this.messages
      .map((msg, index) => ({ index, content: msg.content.toLowerCase() }))
      .filter((entry) => entry.content.includes(query))
      .map((entry) => entry.index);
    this.matchedMessageIndexSet = new Set(this.matchedMessageIndexes);

    this.activeMatchPosition = this.matchedMessageIndexes.length ? 0 : -1;
    this.scrollToActiveSearchMatch();
  }

  goToNextMatch(): void {
    if (!this.matchedMessageIndexes.length) {
      return;
    }
    this.activeMatchPosition =
      (this.activeMatchPosition + 1) % this.matchedMessageIndexes.length;
    this.scrollToActiveSearchMatch();
  }

  goToPreviousMatch(): void {
    if (!this.matchedMessageIndexes.length) {
      return;
    }
    this.activeMatchPosition =
      (this.activeMatchPosition - 1 + this.matchedMessageIndexes.length) %
      this.matchedMessageIndexes.length;
    this.scrollToActiveSearchMatch();
  }

  get searchMatchLabel(): string {
    if (!this.searchQuery.trim() || !this.matchedMessageIndexes.length) {
      return `0 / ${this.matchedMessageIndexes.length}`;
    }
    return `${this.activeMatchPosition + 1} / ${this.matchedMessageIndexes.length}`;
  }

  isMessageMatch(index: number): boolean {
    return this.matchedMessageIndexSet.has(index);
  }

  isActiveSearchMatch(index: number): boolean {
    if (this.activeMatchPosition < 0) {
      return false;
    }
    return this.matchedMessageIndexes[this.activeMatchPosition] === index;
  }

  private scrollToActiveSearchMatch(): void {
    if (this.activeMatchPosition < 0 || !this.messageBubbles.length) {
      return;
    }

    const messageIndex = this.matchedMessageIndexes[this.activeMatchPosition];
    const bubble = this.messageBubbles.get(messageIndex)?.nativeElement;
    if (!bubble) {
      return;
    }

    bubble.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  private parseDecryptedMessageBody(decrypted: string | null): {
    text: string | null;
    clientTimestamp: string | null;
  } {
    if (!decrypted) {
      return { text: null, clientTimestamp: null };
    }

    try {
      const parsed = JSON.parse(decrypted) as Partial<EncryptedMessageBodyV1>;
      if (parsed.v === 1 && typeof parsed.text === 'string') {
        const timestamp =
          typeof parsed.clientTimestamp === 'string' &&
          !Number.isNaN(Date.parse(parsed.clientTimestamp))
            ? parsed.clientTimestamp
            : null;
        return { text: parsed.text, clientTimestamp: timestamp };
      }
    } catch {
      // Backward compatibility for older messages that encrypted raw plaintext only.
    }

    return { text: decrypted, clientTimestamp: null };
  }

  private isDuplicateMessage(message: ChatMessageDto): boolean {
    return this.messages.some(
      (existing) =>
        existing.privateChatId === message.privateChatId &&
        existing.senderUsername === message.senderUsername &&
        existing.timestamp === message.timestamp,
    );
  }
}
