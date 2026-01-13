import * as vscode from 'vscode';
import { initializeLocalization, t } from './localization';
import { CodeOwnerServiceManager } from './codeOwnerServiceManager';
import { StatusBarManager } from './statusBarManager';
import { CodeownerCommands } from './codeownerCommands';

let serviceManager: CodeOwnerServiceManager;
let statusBarManager: StatusBarManager;
let codeownerCommands: CodeownerCommands;
let fileWatchers: vscode.FileSystemWatcher[] = [];

export function activate(context: vscode.ExtensionContext) {
    // Initialize localization
    initializeLocalization(context.extensionPath);

    console.log('Codeowners Lens extension is now active');
    console.log('[CodeOwners] VS Code language:', vscode.env.language);
    console.log('[CodeOwners] Testing localization:', t('No codeowners found'));

    // Check if any workspace folders exist
    if (!vscode.workspace.workspaceFolders || vscode.workspace.workspaceFolders.length === 0) {
        console.log('No workspace folder found, extension will not activate');
        return;
    }

    // Initialize service manager
    serviceManager = new CodeOwnerServiceManager();
    statusBarManager = new StatusBarManager(serviceManager);
    codeownerCommands = new CodeownerCommands(serviceManager, context);

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
                serviceManager.refreshServiceForFile(document.uri);
                if (vscode.window.activeTextEditor) {
                    statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
                }
            }
        })
    );

    // Create file watchers for all workspace folders
    setupFileWatchers(context);

    // Listen for workspace folder changes
    context.subscriptions.push(
        vscode.workspace.onDidChangeWorkspaceFolders(event => {
            // Add new workspace folders
            for (const folder of event.added) {
                serviceManager.addWorkspaceFolder(folder);
            }

            // Remove workspace folders
            for (const folder of event.removed) {
                serviceManager.removeWorkspaceFolder(folder);
            }

            // Recreate file watchers
            disposeFileWatchers();
            setupFileWatchers(context);

            // Update status bar
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        })
    );

    context.subscriptions.push(statusBarManager);
}

function setupFileWatchers(context: vscode.ExtensionContext): void {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
        return;
    }

    for (const folder of workspaceFolders) {
        const pattern = new vscode.RelativePattern(folder, '{.github/,docs/,}CODEOWNERS');
        const fileWatcher = vscode.workspace.createFileSystemWatcher(pattern);

        fileWatcher.onDidChange(uri => {
            serviceManager.refreshServiceForFile(uri);
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        });

        fileWatcher.onDidCreate(uri => {
            serviceManager.refreshServiceForFile(uri);
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        });

        fileWatcher.onDidDelete(uri => {
            serviceManager.refreshServiceForFile(uri);
            if (vscode.window.activeTextEditor) {
                statusBarManager.updateStatusBar(vscode.window.activeTextEditor);
            }
        });

        fileWatchers.push(fileWatcher);
        context.subscriptions.push(fileWatcher);
    }
}

function disposeFileWatchers(): void {
    for (const watcher of fileWatchers) {
        watcher.dispose();
    }
    fileWatchers = [];
}

export function deactivate() {
    // Cleanup is handled by context.subscriptions
}
