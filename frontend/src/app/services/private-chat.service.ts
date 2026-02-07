import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { PrivateChatDto } from '../models/dtos/PrivateChatDto';
import { environment } from '../../environments/environment';
import { ChatMessageDto } from '../models/dtos/ChatMessageDto';
import { BatchOperationResponse } from '../models/dtos/BatchOperationResponse';
import { CreateGroupFromChatsRequest } from '../models/dtos/CreateGroupFromChatsRequest';

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

  getUserPrivateChats() {
    return this.http.get<PrivateChatDto[]>(
      `${this.apiUrl}/private-chats/user-chats`,
    );
  }

  clearMultiplePrivateChats(privatechatIds: number[]) {
    return this.http.delete<BatchOperationResponse>(
      `${this.apiUrl}/private-chats/clear-multiple`,
      {
        body: privatechatIds,
      },
    );
  }

  createGroupFromChats(
    privateChatIds: number[],
    groupName: string,
    description: string,
  ) {
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
