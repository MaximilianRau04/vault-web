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

  messages: ChatMessageDto[] = [];
  newMessage = '';

  @ViewChild('messageContainer') messageContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;
  private privateMessageSub!: Subscription;

  private shouldScroll = false;
  isSearchOpen = false;
  searchQuery = '';
  filteredMessages: ChatMessageDto[] = [];

  constructor(
    private wsService: WebSocketService,
    private chatService: PrivateChatService,
  ) {}

  ngOnInit(): void {
    this.chatService.getMessages(this.privateChatId).subscribe({
      next: (msgs) => {
        this.messages = msgs;
        this.applySearch();
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
          this.messages.push(msg);
          this.applySearch();
          this.shouldScroll = !this.searchQuery.trim();
        }
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

    const message: ChatMessageDto = {
      content: this.newMessage,
      timestamp: new Date().toISOString(),
      senderUsername: this.currentUsername ? this.currentUsername : 'Unknown',
      privateChatId: this.privateChatId,
    };

    this.wsService.sendPrivateMessage(message);

    this.newMessage = '';
  }

  onClose(): void {
    this.closeChat.emit();
  }

  toggleSearch() {
    this.isSearchOpen = !this.isSearchOpen;
    if (this.isSearchOpen) {
      setTimeout(() => this.searchInput?.nativeElement.focus(), 0);
    } else {
      this.searchQuery = '';
      this.applySearch(); // reset filteredMessages to all messages
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
