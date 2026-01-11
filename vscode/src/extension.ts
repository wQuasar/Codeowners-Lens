import * as vscode from 'vscode';
import { CodeOwnerService } from './codeOwnerService';
import { StatusBarManager } from './statusBarManager';
import { CodeownerCommands } from './codeownerCommands';

let codeOwnerService: CodeOwnerService;
let statusBarManager: StatusBarManager;
let codeownerCommands: CodeownerCommands;

export function activate(context: vscode.ExtensionContext) {
    console.log('Codeowners Lens extension is now active');

    // Get workspace root
    const workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? null;

    if (!workspaceRoot) {
        console.log('No workspace folder found, extension will not activate');
        return;
    }

    // Initialize services
    codeOwnerService = new CodeOwnerService(workspaceRoot);
    statusBarManager = new StatusBarManager(codeOwnerService);
    codeownerCommands = new CodeownerCommands(codeOwnerService, context);

    // Update status bar for the active editor
    if (vscode.window.activeTextEditor) {
        statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
    }

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('codeowners-lens.showCodeownerRule', () => {
            codeownerCommands.showCodeownerRule();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('codeowners-lens.showCodeowners', () => {
            codeownerCommands.showCodeownersForChangedFiles();
        })
    );

    context.subscriptions.push(
        vscode.commands.registerCommand('codeowners-lens.refreshCodeowners', () => {
            codeownerCommands.refreshCodeowners();
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        })
    );

    // Listen for active editor changes
    context.subscriptions.push(
        vscode.window.onDidChangeActiveTextEditor(editor => {
            statusBarManager.updateStatusBar(editor);
        })
    );

    // Listen for file changes to refresh CODEOWNERS
    context.subscriptions.push(
        vscode.workspace.onDidSaveTextDocument(document => {
            if (document.uri.fsPath.includes('CODEOWNERS')) {
                codeOwnerService.refreshCodeOwnerRules();
                if (vscode.window.activeTextEditor) {
                    statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
                }
            }
        })
    );

    // Listen for file system changes
    const fileWatcher = vscode.workspace.createFileSystemWatcher('**/{.github/,docs/,}CODEOWNERS');

    context.subscriptions.push(
        fileWatcher.onDidChange(() => {
            codeOwnerService.refreshCodeOwnerRules();
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        })
    );

    context.subscriptions.push(
        fileWatcher.onDidCreate(() => {
            codeOwnerService.refreshCodeOwnerRules();
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        })
    );

    context.subscriptions.push(
        fileWatcher.onDidDelete(() => {
            codeOwnerService.refreshCodeOwnerRules();
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        })
    );

    context.subscriptions.push(fileWatcher);
    context.subscriptions.push(statusBarManager);
}

export function deactivate() {
    // Cleanup is handled by context.subscriptions
}
