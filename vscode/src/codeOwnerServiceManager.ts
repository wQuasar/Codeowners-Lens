import * as vscode from 'vscode';
import { CodeOwnerService } from './codeOwnerService';

export class CodeOwnerServiceManager {
    private services: Map<string, CodeOwnerService> = new Map();

    constructor() {
        this.initializeServices();
    }

    private initializeServices(): void {
        const workspaceFolders = vscode.workspace.workspaceFolders;
        if (!workspaceFolders) {
            return;
        }

        for (const folder of workspaceFolders) {
            this.addWorkspaceFolder(folder);
        }
    }

    public addWorkspaceFolder(folder: vscode.WorkspaceFolder): void {
        const key = folder.uri.fsPath;
        if (!this.services.has(key)) {
            this.services.set(key, new CodeOwnerService(folder.uri.fsPath));
        }
    }

    public removeWorkspaceFolder(folder: vscode.WorkspaceFolder): void {
        const key = folder.uri.fsPath;
        this.services.delete(key);
    }

    public getServiceForFile(uri: vscode.Uri): CodeOwnerService | null {
        const workspaceFolder = vscode.workspace.getWorkspaceFolder(uri);
        if (!workspaceFolder) {
            return null;
        }

        return this.services.get(workspaceFolder.uri.fsPath) ?? null;
    }

    public getServiceForWorkspace(workspaceFolder: vscode.WorkspaceFolder): CodeOwnerService | null {
        return this.services.get(workspaceFolder.uri.fsPath) ?? null;
    }

    public getAllServices(): CodeOwnerService[] {
        return Array.from(this.services.values());
    }

    public refreshAllServices(): void {
        for (const service of this.services.values()) {
            service.refreshCodeOwnerRules();
        }
    }

    public refreshServiceForFile(uri: vscode.Uri): void {
        const service = this.getServiceForFile(uri);
        if (service) {
            service.refreshCodeOwnerRules();
        }
    }

    public hasAnyWorkspace(): boolean {
        return this.services.size > 0;
    }

    public dispose(): void {
        this.services.clear();
    }
}
