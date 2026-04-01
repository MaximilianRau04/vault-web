import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MenuItem, ConfirmationService } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { Menu } from 'primeng/menu';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { FileDto } from '../../models/dtos/FileDto';
import { FolderDto } from '../../models/dtos/FolderDto';
import { CloudService } from '../../services/cloud.service';
import { finalize, firstValueFrom } from 'rxjs';

interface Breadcrumb {
  name: string;
  path: string;
}

interface CloudEntry {
  kind: 'folder' | 'file';
  name: string;
  path: string;
  sizeLabel: string;
  typeLabel: string;
}

@Component({
  selector: 'app-cloud',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    ToolbarModule,
    MenuModule,
    BreadcrumbModule,
    DialogModule,
    InputTextModule,
    ConfirmDialogModule,
  ],
  providers: [ConfirmationService],
  templateUrl: './cloud.component.html',
  styleUrls: ['./cloud.component.scss'],
})
export class CloudComponent implements OnInit {
  @ViewChild('fileUploadInput') fileUploadInput?: ElementRef<HTMLInputElement>;

  currentFolder?: FolderDto;
  loading = true;
  error?: string;
  breadcrumbs: Breadcrumb[] = [];
  rootPath = '';

  showFileEditor = false;
  editingFile: any = null;
  newFileName = '';
  fileContent = '';

  showCreateFolderDialog = false;
  showRenameFolderDialog = false;
  showRenameFileDialog = false;
  newFolderName = '';
  renameFolderName = '';
  renameFileName = '';
  selectedFolderPathForRename: string | null = null;
  selectedFolderNameForRename: string | null = null;
  selectedFileForRename: FileDto | null = null;

  createMenuItems: MenuItem[] = [];
  entries: CloudEntry[] = [];
  downloadingPaths = new Set<string>();

  private draggedPath: string | null = null;
  private draggedIsFolder = false;

  constructor(
    private cloudService: CloudService,
    private confirmationService: ConfirmationService,
  ) {}

  ngOnInit(): void {
    this.createMenuItems = [
      {
        label: 'New Folder',
        icon: 'pi pi-folder',
        command: () => this.openCreateFolderDialog(),
      },
      {
        label: 'New File',
        icon: 'pi pi-file',
        command: () => this.createNewFile(),
      },
      {
        label: 'Upload',
        icon: 'pi pi-upload',
        command: () => this.openFileUploadDialog(),
      },
    ];
    this.loadRootFolder();
  }

  get breadcrumbItems(): MenuItem[] {
    return this.breadcrumbs.map((crumb) => ({
      label: crumb.name,
      command: () => this.navigateToFolder(crumb.path),
    }));
  }

  get homeBreadcrumb(): MenuItem {
    return {
      icon: 'pi pi-home',
      command: () => this.navigateToRoot(),
    };
  }

  get totalItemsInView(): number {
    if (!this.currentFolder) return 0;
    return this.currentFolder.folders.length + this.currentFolder.files.length;
  }

  private buildEntries(folder: FolderDto): CloudEntry[] {
    const folderEntries: CloudEntry[] = folder.folders.map((entryFolder) => ({
      kind: 'folder',
      name: entryFolder.name,
      path: entryFolder.path,
      sizeLabel: `${entryFolder.folders.length + entryFolder.files.length} items`,
      typeLabel: 'Folder',
    }));

    const fileEntries: CloudEntry[] = folder.files.map((entryFile) => ({
      kind: 'file',
      name: entryFile.name,
      path: entryFile.path,
      sizeLabel: this.formatFileSize(entryFile.size),
      typeLabel: entryFile.mimeType || 'Unknown',
    }));

    return [...folderEntries, ...fileEntries];
  }

  loadRootFolder() {
    this.loading = true;
    this.error = undefined;
    this.cloudService.getRootFolder().subscribe({
      next: (folder) => {
        this.currentFolder = folder;
        this.entries = this.buildEntries(folder);
        this.rootPath = folder.path;
        this.updateBreadcrumbs(folder.path);
        this.loading = false;
      },
      error: () => {
        this.error = 'Error loading root folder';
        this.loading = false;
      },
    });
  }

  reloadRootFolder() {
    this.loadRootFolder();
  }

  navigateToFolder(folderPath?: string) {
    this.loading = true;
    const relativePath = this.getRelativePath(folderPath || this.rootPath);
    this.cloudService.getFolderByPath(relativePath).subscribe({
      next: (folder) => {
        this.currentFolder = folder;
        this.entries = this.buildEntries(folder);
        this.updateBreadcrumbs(folder.path);
        this.loading = false;
      },
      error: () => {
        this.error = 'Error navigating to folder';
        this.loading = false;
      },
    });
  }

  navigateToRoot() {
    this.navigateToFolder(this.rootPath);
  }

  updateBreadcrumbs(currentPath: string) {
    this.breadcrumbs = [];
    const relativePath = currentPath
      .replace(this.rootPath, '')
      .replace(/^[\\/]/, '');
    if (!relativePath) return;

    const parts = relativePath.split(/[\\/]/);
    let accumulatedPath = this.rootPath;

    parts.forEach((part) => {
      accumulatedPath = accumulatedPath + '/' + part;
      this.breadcrumbs.push({ name: part, path: accumulatedPath });
    });
  }

  getRelativePath(fullPath: string): string {
    const normalizedPath = (fullPath || '').replace(/\\/g, '/').trim();
    const normalizedRoot = (this.rootPath || '').replace(/\\/g, '/').trim();

    if (!normalizedPath || normalizedPath === '/' || normalizedPath === '.') {
      return '/';
    }

    if (
      normalizedPath === normalizedRoot ||
      (normalizedRoot === '.' && normalizedPath === '.')
    ) {
      return '/';
    }

    const rootPrefix =
      normalizedRoot && normalizedRoot !== '.' && normalizedRoot !== '/'
        ? `${normalizedRoot}/`
        : '';

    let relative = normalizedPath;
    if (rootPrefix && relative.startsWith(rootPrefix)) {
      relative = relative.substring(rootPrefix.length);
    }

    relative = relative.replace(/^\/+/, '');
    return relative || '/';
  }

  toggleCreateMenu(event: Event, menu: Menu) {
    menu.toggle(event);
  }

  openFileUploadDialog() {
    this.fileUploadInput?.nativeElement.click();
  }

  openCreateFolderDialog() {
    this.newFolderName = '';
    this.showCreateFolderDialog = true;
  }

  createNewFolder() {
    const folderName = this.newFolderName.trim();
    if (!folderName) return;
    const currentPath = this.getRelativePath(this.currentFolder?.path || '/');
    this.cloudService.createFolder(currentPath, folderName).subscribe({
      next: () => {
        this.showCreateFolderDialog = false;
        this.navigateToFolder(this.currentFolder?.path);
      },
      error: (err) => alert('Error creating folder: ' + err.message),
    });
  }

  createNewFile() {
    this.editingFile = null;
    this.newFileName = '';
    this.fileContent = '';
    this.showFileEditor = true;
  }

  private isTextEditable(fileName: string): boolean {
    const ext = fileName.split('.').pop()?.toLowerCase();
    const textExt = [
      'txt',
      'md',
      'json',
      'xml',
      'log',
      'csv',
      'ts',
      'js',
      'scss',
      'css',
      'html',
      'yml',
      'yaml',
      'java',
      'py',
      'sql',
    ];
    return !!ext && textExt.includes(ext);
  }

  onEditAction(path: string, name: string) {
    const file = this.toFileRef(path, name);
    if (this.isTextEditable(name)) {
      this.editFile(file);
      return;
    }
    this.openRenameFileDialog(file);
  }

  editFile(file: any) {
    if (!this.isTextEditable(file.name)) {
      this.openRenameFileDialog(file);
      return;
    }

    this.editingFile = file;
    this.newFileName = file.name;
    const relativePath = this.getRelativePath(file.path);

    this.cloudService.getFileContent(relativePath).subscribe({
      next: (content) => {
        this.fileContent = content;
        this.showFileEditor = true;
      },
      error: (err) => {
        this.editingFile = null;
        alert('Error loading file: ' + err.message);
      },
    });
  }

  async saveFile() {
    const nameToSave = this.newFileName.trim();
    if (!nameToSave) return;

    try {
      if (this.editingFile && nameToSave !== this.editingFile.name) {
        const relativeSource = this.getRelativePath(this.editingFile.path);
        const relativeTargetDir = this.getParentRelativePath(this.editingFile.path);
        const relativeTarget = this.joinRelativePath(relativeTargetDir, nameToSave);
        await firstValueFrom(
          this.cloudService.renameOrMoveFile(relativeSource, relativeTarget),
        );
      }

      const currentPath = this.getRelativePath(this.currentFolder?.path || '/');
      const fileBlob = new Blob([this.fileContent], { type: 'text/plain' });
      const file = new File([fileBlob], nameToSave);
      await firstValueFrom(this.cloudService.uploadFile(currentPath, file));

      this.navigateToFolder(this.currentFolder?.path);
      this.closeFileEditor();
    } catch (err: any) {
      alert('Error saving file: ' + err.message);
    }
  }

  uploadFile(folderPath: string, file: File) {
    this.cloudService.uploadFile(folderPath, file).subscribe({
      next: () => {
        this.navigateToFolder(this.currentFolder?.path);
        this.closeFileEditor();
      },
      error: (err) => alert('Error uploading file: ' + err.message),
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file) return;

    const currentPath = this.getRelativePath(this.currentFolder?.path || '/');
    this.uploadFile(currentPath, file);
    input.value = '';
  }

  confirmDeleteFolder(folderPath: string) {
    this.confirmationService.confirm({
      header: 'Delete Folder',
      message: 'Do you really want to delete this folder?',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-text p-button-sm',
      accept: () => this.deleteFolder(folderPath),
    });
  }

  deleteFolder(folderPath: string) {
    const relativePath = this.getRelativePath(folderPath);
    this.cloudService.deleteFolder(relativePath).subscribe({
      next: () => this.navigateToFolder(this.currentFolder?.path),
      error: (err) => alert('Error deleting folder: ' + err.message),
    });
  }

  confirmDeleteFile(filePath: string) {
    const fileName = this.getNameFromPath(filePath);
    this.confirmationService.confirm({
      header: 'Delete File',
      message: `Do you really want to delete "${fileName}"?`,
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger p-button-sm',
      rejectButtonStyleClass: 'p-button-text p-button-sm',
      accept: () => this.deleteFile(filePath),
    });
  }

  deleteFile(filePath: string) {
    const relativePath = this.getRelativePath(filePath);
    this.cloudService.deleteFile(relativePath).subscribe({
      next: () => this.navigateToFolder(this.currentFolder?.path),
      error: (err) => alert('Error deleting file: ' + err.message),
    });
  }

  openRenameFolderDialog(folderPath: string, folderName: string) {
    this.selectedFolderPathForRename = folderPath;
    this.selectedFolderNameForRename = folderName;
    this.renameFolderName = folderName;
    this.showRenameFolderDialog = true;
  }

  renameFolder() {
    const folderPath = this.selectedFolderPathForRename;
    const folderName = this.selectedFolderNameForRename;
    const newName = this.renameFolderName.trim();
    if (!folderPath || !folderName || !newName || newName === folderName) return;

    const relativeSource = this.getRelativePath(folderPath);
    const relativeTargetDir = this.getParentRelativePath(folderPath);
    const relativeTarget = this.joinRelativePath(relativeTargetDir, newName);

    this.cloudService
      .renameOrMoveFolder(relativeSource, relativeTarget)
      .subscribe({
        next: () => {
          this.showRenameFolderDialog = false;
          this.selectedFolderPathForRename = null;
          this.selectedFolderNameForRename = null;
          this.navigateToFolder(this.currentFolder?.path);
        },
        error: (err) => alert('Error renaming folder: ' + err.message),
      });
  }

  openRenameFileDialog(file: FileDto) {
    this.selectedFileForRename = file;
    this.renameFileName = file.name;
    this.showRenameFileDialog = true;
  }

  renameFile() {
    const file = this.selectedFileForRename;
    const newName = this.renameFileName.trim();
    if (!file || !newName || newName === file.name) return;

    const relativeSource = this.getRelativePath(file.path);
    const relativeTargetDir = this.getParentRelativePath(file.path);
    const relativeTarget = this.joinRelativePath(relativeTargetDir, newName);

    this.cloudService.renameOrMoveFile(relativeSource, relativeTarget).subscribe({
      next: () => {
        this.showRenameFileDialog = false;
        this.selectedFileForRename = null;
        this.navigateToFolder(this.currentFolder?.path);
      },
      error: (err) => alert('Error renaming file: ' + err.message),
    });
  }

  downloadFile(file: any) {
    const pathKey = file.path;
    const relativePath = this.getRelativePath(file.path);
    this.downloadingPaths.add(pathKey);
    this.cloudService
      .getFileBlob(relativePath)
      .pipe(
        finalize(() => {
          this.downloadingPaths.delete(pathKey);
        }),
      )
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = file.name;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: (err) => alert('Error downloading file: ' + err.message),
      });
  }

  isDownloading(path: string): boolean {
    return this.downloadingPaths.has(path);
  }

  private toFileRef(path: string, name: string): FileDto {
    return { path, name, size: 0, mimeType: '' };
  }

  downloadFileByPath(path: string, name: string) {
    this.downloadFile(this.toFileRef(path, name));
  }

  previewFileByPath(path: string, name: string) {
    this.previewFile(this.toFileRef(path, name));
  }

  closeFileEditor() {
    this.showFileEditor = false;
    this.editingFile = null;
    this.newFileName = '';
    this.fileContent = '';
  }

  private getParentRelativePath(fullPath: string): string {
    const relative = this.getRelativePath(fullPath);
    if (!relative || relative === '/') return '/';
    const lastSlash = relative.lastIndexOf('/');
    if (lastSlash <= 0) return '/';
    return relative.substring(0, lastSlash);
  }

  private joinRelativePath(parentPath: string, name: string): string {
    if (!parentPath || parentPath === '/') return name;
    return `${parentPath}/${name}`;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  onDragStart(event: DragEvent, path: string, isFolder: boolean) {
    this.draggedPath = path;
    this.draggedIsFolder = isFolder;
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/plain', path);
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  }

  async onDrop(event: DragEvent, targetFolderPath?: string | null) {
    event.preventDefault();
    if (!this.draggedPath) return;

    const targetPath = targetFolderPath || this.currentFolder?.path;
    if (!targetPath || this.draggedPath === targetPath) return;

    const relativeSource = this.getRelativePath(this.draggedPath);
    const relativeTarget = this.getRelativePath(targetPath);

    try {
      if (this.draggedIsFolder) {
        await this.cloudService
          .renameOrMoveFolder(
            relativeSource,
            `${relativeTarget}/${this.getNameFromPath(this.draggedPath)}`,
          )
          .toPromise();
      } else {
        await this.cloudService
          .renameOrMoveFile(
            relativeSource,
            `${relativeTarget}/${this.getNameFromPath(this.draggedPath)}`,
          )
          .toPromise();
      }
      this.reloadRootFolder();
    } catch (err: any) {
      alert('Error moving item: ' + err.message);
    } finally {
      this.draggedPath = null;
    }
  }

  onBreadcrumbDragOver(event: DragEvent) {
    event.preventDefault();
  }

  async onBreadcrumbDrop(event: DragEvent, targetPath: string) {
    event.preventDefault();
    if (!this.draggedPath) return;

    const relativeSource = this.getRelativePath(this.draggedPath);
    const relativeTarget = this.getRelativePath(targetPath);

    try {
      if (this.draggedIsFolder) {
        await this.cloudService
          .renameOrMoveFolder(
            relativeSource,
            `${relativeTarget}/${this.getNameFromPath(this.draggedPath)}`,
          )
          .toPromise();
      } else {
        await this.cloudService
          .renameOrMoveFile(
            relativeSource,
            `${relativeTarget}/${this.getNameFromPath(this.draggedPath)}`,
          )
          .toPromise();
      }
      this.reloadRootFolder();
    } catch (err: any) {
      alert('Error moving item: ' + err.message);
    } finally {
      this.draggedPath = null;
    }
  }

  getNameFromPath(path: string): string {
    const parts = path.split('/');
    return parts[parts.length - 1];
  }

  previewFile(file: any) {
    const ext = file.name.split('.').pop()?.toLowerCase();
    const imageExt = ['png', 'jpg', 'jpeg', 'gif', 'bmp'];
    const pdfExt = ['pdf'];
    const textExt = ['txt', 'md', 'json', 'xml', 'log'];

    if (ext && (imageExt.includes(ext) || pdfExt.includes(ext))) {
      const relativePath = this.getRelativePath(file.path);
      this.cloudService.getFileView(relativePath).subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank');
        },
        error: (err) => alert('Error previewing file: ' + err.message),
      });
    } else if (ext && textExt.includes(ext)) {
      this.editFile(file);
    } else {
      this.downloadFile(file);
    }
  }
}
