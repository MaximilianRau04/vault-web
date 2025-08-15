export interface ChatMessageDto {
  content: string;
  senderUsername?: string;
  groupId?: number | null;
  privateChatId?: number;
  timestamp: string;
}
