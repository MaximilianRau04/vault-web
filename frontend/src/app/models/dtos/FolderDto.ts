import { FileDto } from './FileDto';
import { FolderListItemDto } from './FolderListItemDto';

export interface FolderDto {
  name: string;
  path: string;
  folders: FolderListItemDto[];
  files: FileDto[];
  lastModifiedAt: number;
}
