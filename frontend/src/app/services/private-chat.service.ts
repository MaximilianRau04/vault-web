import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PrivateChatDto } from '../models/dtos/PrivateChatDto';
import { environment } from '../../environments/environment';
import { ChatMessageDto } from '../models/dtos/ChatMessageDto';
import { BatchOperationResponse } from '../models/dtos/BatchOperationResponse';
import { CreateGroupFromChatsRequest } from '../models/dtos/CreateGroupFromChatsRequest';
import { ClearChatRequestDto } from '../models/dtos/ClearChatRequestDto';

@Injectable({
  providedIn: 'root',
})
export class PrivateChatService {
  private apiUrl = environment.mainApiUrl;

  constructor(private http: HttpClient) {}

  getOrCreatePrivateChat(
    username1: string,
    username2: string,
  ): Observable<PrivateChatDto> {
    return this.http.get<PrivateChatDto>(
      `${this.apiUrl}/private-chats/between?sender=${username1}&receiver=${username2}`,
    );
  }

  getMessages(privateChatId: number): Observable<ChatMessageDto[]> {
    return this.http.get<ChatMessageDto[]>(
      `${this.apiUrl}/private-chats/private?privateChatId=${privateChatId}`,
    );
  }

  getUserPrivateChats(): Observable<PrivateChatDto[]> {
    return this.http.get<PrivateChatDto[]>(
      `${this.apiUrl}/private-chats/user-chats`,
    );
  }

  clearMultiplePrivateChats(
    privateChatIds: number[],
  ): Observable<BatchOperationResponse> {
    const request: ClearChatRequestDto = {
      privateChatIds,
    };
    return this.http.post<BatchOperationResponse>(
      `${this.apiUrl}/private-chats/clear-multiple`,
      request,
    );
  }

  createGroupFromChats(
    privateChatIds: number[],
    groupName: string,
    description: string,
  ): Observable<BatchOperationResponse> {
    const request: CreateGroupFromChatsRequest = {
      privateChatIds,
      groupName,
      description,
    };
    return this.http.post<BatchOperationResponse>(
      `${this.apiUrl}/private-chats/create-group-from-chats`,
      request,
    );
  }
}
