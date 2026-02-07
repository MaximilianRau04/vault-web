import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserDto } from '../../models/dtos/UserDto';
import { CommonModule } from '@angular/common';
import { PrivateChatDialogComponent } from '../private-chat-dialog/private-chat-dialog.component';
import { PrivateChatService } from '../../services/private-chat.service';
import { PrivateChatDto } from '../../models/dtos/PrivateChatDto';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, PrivateChatDialogComponent, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  users: UserDto[] = [];
  privateChats: PrivateChatDto[] = [];
  isLoading = true;
  error: string | null = null;
  selectedUsername: string | null = null;
  privateChatId: number | null = null;

  currentUsername: string | null = null;
  isEditMode = false;
  selectedChatIds: Set<number> = new Set();
  showClearConfirmDialog = false;
  isProcessing = false;
  showGroupDialog: boolean = false;
  newGroupName = '';
  groupDescription = '';

  constructor(
    private userService: UserService,
    private privateChatService: PrivateChatService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUsername = this.authService.getUsername();

    if (!this.currentUsername) {
      this.router.navigate(['/login']);
      this.error = 'User not logged in.';
      this.isLoading = false;
      return;
    }
    /*
        this.userService.getAllUsers().subscribe({
          next: (data) => {
            this.users = data;
            this.isLoading = false;
          },
          error: () => {
            this.error = 'Failed to load users.';
            this.isLoading = false;
          },
        });
     */

    this.loadData();
  }

  private loadData() {
    this.isLoading = true;

    Promise.all([
      this.userService.getAllUsers().toPromise(),
      this.privateChatService.getUserPrivateChats().toPromise(),
    ])
      .then(([users, chats]) => {
        this.users = users || [];
        this.privateChats = chats || [];
        this.isLoading = false;
      })
      .catch(() => {
        this.error = 'Failed to Load data.';
        this.isLoading = false;
      });
  }

  openChat(username: string): void {
    if (!this.currentUsername) return;

    this.selectedUsername = username;

    this.privateChatService
      .getOrCreatePrivateChat(this.currentUsername, username)
      .subscribe({
        next: (chat: PrivateChatDto) => {
          this.privateChatId = chat.id;
        },
        error: () => {
          console.error('Failed to get or create private chat');
        },
      });
  }

  closeChat(): void {
    this.selectedUsername = null;
    this.privateChatId = null;
  }

  toggleEditMode() {
    this.isEditMode = !this.isEditMode;
    if (!this.isEditMode) {
      this.selectedChatIds.clear();
    }
  }

  toggleChatSelection(chatId: number) {
    if (this.selectedChatIds.has(chatId)) {
      this.selectedChatIds.delete(chatId);
    } else {
      this.selectedChatIds.add(chatId);
    }
  }

  isChatSelected(chatId: number): boolean {
    return this.selectedChatIds.has(chatId);
  }

  hasSelectedChats() {
    console.log('Selected items size ' + this.selectedChatIds.size);
    return this.selectedChatIds.size > 0;
  }

  openPrivateChat(chat: PrivateChatDto) {
    if (this.isEditMode) return; //Don't open chat in edit mode
    if (!this.currentUsername) return;

    //get other user info
    const otherUserName =
      chat.username1 === this.currentUsername ? chat.username2 : chat.username1;
    this.selectedUsername = otherUserName;
    this.privateChatId = chat.id;
  }

  getOtherUsername(chat: PrivateChatDto): string {
    if (!this.currentUsername) return '';

    //get other user info
    return chat.username1 === this.currentUsername
      ? chat.username2
      : chat.username1;
  }

  openClearConfirmDialog() {
    if (this.hasSelectedChats()) {
      this.showClearConfirmDialog = true;
    }
  }

  cancelDialog() {
    this.showClearConfirmDialog = false;
    this.showGroupDialog = false;
    this.newGroupName = '';
    this.groupDescription = '';
  }

  confirmClearChats() {
    this.isProcessing = true;
    const chatIds = Array.from(this.selectedChatIds);
    this.privateChatService.clearMultiplePrivateChats(chatIds).subscribe({
      next: (response) => {
        this.showClearConfirmDialog = false;
        this.selectedChatIds.clear();
        this.isProcessing = false;
        this.isEditMode = false;
        //Reload data to reflect changes
        this.loadData();
      },
      error: (err) => {
        console.error('Failed to clear chats ', err);
        this.error = 'Failed to clear chats. Please try again.';
        this.isProcessing = false;
      },
    });
  }

  openCreateGroupDialog() {
    if (this.hasSelectedChats()) {
      this.showGroupDialog = true;
      this.newGroupName = '';
    }
  }

  confirmCreateGroup() {
    if (!this.newGroupName.trim()) {
      return;
    }
    this.isProcessing = true;
    const chatIds = Array.from(this.selectedChatIds);
    this.privateChatService
      .createGroupFromChats(chatIds, this.newGroupName, this.groupDescription)
      .subscribe({
        next: (respose) => {
          console.log('Group created: ', respose.groupId);
          this.showGroupDialog = false;
          this.isProcessing = false;
          this.isEditMode = false;
          this.newGroupName = '';
          this.groupDescription = '';
          this.selectedChatIds.clear();
          this.loadData();
        },
        error: (err) => {
          this.error = 'Failed to create group. Please Try again.';
          this.isProcessing = false;
        },
      });
  }
}
