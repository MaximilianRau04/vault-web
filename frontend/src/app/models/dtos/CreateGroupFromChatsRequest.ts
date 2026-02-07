export interface CreateGroupFromChatsRequest {
  privateChatIds: number[];
  groupName: string;
  description?: string;
}
