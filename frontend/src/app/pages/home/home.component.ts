import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserDto } from '../../models/dtos/UserDto';
import { CommonModule } from '@angular/common';
import { PrivateChatDialogComponent } from '../private-chat-dialog/private-chat-dialog.component';
import { PrivateChatService } from '../../services/private-chat.service';
import { PrivateChatDto } from '../../models/dtos/PrivateChatDto';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, PrivateChatDialogComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  users: UserDto[] = [];
  isLoading = true;
  error: string | null = null;
  selectedUsername: string | null = null;
  privateChatId: number | null = null;

  currentUsername: string | null = null;

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
}
