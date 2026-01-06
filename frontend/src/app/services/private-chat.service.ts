import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { PrivateChatDto } from '../models/dtos/PrivateChatDto';
import { environment } from '../../environments/environment';
import { ChatMessageDto } from '../models/dtos/ChatMessageDto';
import { DeviceDto } from '../models/dtos/DeviceDto';

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

  getDevices(privateChatId: number): Observable<DeviceDto[]> {
    return this.http.get<DeviceDto[]>(
      `${this.apiUrl}/private-chats/devices?privateChatId=${privateChatId}`,
    );
  }
}
