import * as vscode from 'vscode';
import { t } from './localization';
import { CodeOwnerServiceManager } from './codeOwnerServiceManager';
import { FileCodeOwnerStateType } from './codeOwnerService';
import { CodeOwnerRule } from './codeOwnerRule';
import * as fs from 'fs';

export class CodeownerCommands {
    constructor(
        private serviceManager: CodeOwnerServiceManager,
        private context: vscode.ExtensionContext
    ) {}

    public async showCodeownerRule(): Promise<void> {
        const editor = vscode.window.activeTextEditor;
        if (!editor) {
            return;
        }

        const service = this.serviceManager.getServiceForFile(editor.document.uri);
        if (!service) {
            return;
        }

        const fileState = service.getFileCodeOwnerState(editor.document.uri);

        if (fileState.type !== FileCodeOwnerStateType.RuleFoundInCodeOwnerFile || !fileState.codeOwnerRule) {
            vscode.window.showInformationMessage(t('No codeowners found'));
            return;
        }

        const rule = fileState.codeOwnerRule;
        const owners = rule.owners;

        if (owners.length === 1) {
            await this.navigateToCodeownerRule(service, rule, owners[0]);
        } else {
            const selectedOwner = await vscode.window.showQuickPick(owners, {
                placeHolder: 'Select a codeowner to navigate to the rule'
            });

            if (selectedOwner) {
                await this.navigateToCodeownerRule(service, rule, selectedOwner);
            }
        }
    }

    private async navigateToCodeownerRule(service: any, rule: CodeOwnerRule, ownerLabel: string): Promise<void> {
        const codeOwnerFile = service.getCodeOwnerFile();
        if (!codeOwnerFile) {
            return;
        }

        const trueOwner = service.getTrueCodeOwner(ownerLabel);
        const columnIndex = this.getColumnIndexForCodeOwner(codeOwnerFile, rule.lineNumber, trueOwner);

        const document = await vscode.workspace.openTextDocument(codeOwnerFile);
        const editor = await vscode.window.showTextDocument(document);

        const position = new vscode.Position(
            rule.lineNumber,
            columnIndex + (trueOwner.length - ownerLabel.length)
        );

        editor.selection = new vscode.Selection(position, position);
        editor.revealRange(
            new vscode.Range(position, position),
            vscode.TextEditorRevealType.InCenter
        );
    }

    private getColumnIndexForCodeOwner(filePath: string, lineNumber: number, codeOwnerLabel: string): number {
        try {
            const content = fs.readFileSync(filePath, 'utf-8');
            const lines = content.split('\n');
            const line = lines[lineNumber];

            if (!line) {
                return 0;
            }

            const pattern = new RegExp(`(?<=^|\\s)${this.escapeRegExp(codeOwnerLabel)}(?=\\s|$)`);
            const match = pattern.exec(line);
            return match ? match.index : 0;
        } catch (error) {
            return 0;
        }
    }

    private escapeRegExp(str: string): string {
        return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    public async showCodeownersForChangedFiles(): Promise<void> {
        const gitExtension = vscode.extensions.getExtension('vscode.git');
        if (!gitExtension) {
            vscode.window.showErrorMessage('Git extension is not available');
            return;
        }

        if (!gitExtension.isActive) {
            await gitExtension.activate();
        }

        const git = gitExtension.exports.getAPI(1);
        const repositories = git.repositories;

        if (repositories.length === 0) {
            vscode.window.showInformationMessage('No Git repository found');
            return;
        }

        const repository = repositories[0];
        const changes = [
            ...repository.state.workingTreeChanges,
            ...repository.state.indexChanges
        ];

        if (changes.length === 0) {
            vscode.window.showInformationMessage(t('No files modified.'));
            return;
        }

        // Group files by codeowners
        const codeownerMap = new Map<string, vscode.Uri[]>();
        let hasCodeOwnersFile = false;

        for (const change of changes) {
            if (!change.uri) {
                continue;
            }

            const service = this.serviceManager.getServiceForFile(change.uri);
            if (service) {
                hasCodeOwnersFile = true;
                const fileState = service.getFileCodeOwnerState(change.uri);

                if (fileState.type === FileCodeOwnerStateType.RuleFoundInCodeOwnerFile && fileState.codeOwnerRule) {
                    const owners = fileState.codeOwnerRule.owners;
                    const key = owners.length > 0 ? owners.join(', ') : '¯\\_(ツ)_/¯';

                    if (!codeownerMap.has(key)) {
                        codeownerMap.set(key, []);
                    }
                    codeownerMap.get(key)?.push(change.uri);
                } else {
                    const key = '¯\\_(ツ)_/¯';
                    if (!codeownerMap.has(key)) {
                        codeownerMap.set(key, []);
                    }
                    codeownerMap.get(key)?.push(change.uri);
                }
            }
        }

        if (!hasCodeOwnersFile) {
            vscode.window.showWarningMessage(t('No CODEOWNERS file found.'));
        }

        // Check if CODEOWNERS file was edited
        const codeownerFileEdited = changes.some(change =>
            change.uri.fsPath.includes('CODEOWNERS')
        );

        if (codeownerFileEdited) {
            vscode.window.showWarningMessage(t('CODEOWNERS file is edited.'));
        }

        // Create quick pick items
        const items: vscode.QuickPickItem[] = [];

        for (const [owners, files] of codeownerMap.entries()) {
            items.push({
                label: `$(organization) ${owners}`,
                description: `${files.length} file(s)`,
                detail: files.map(uri => vscode.workspace.asRelativePath(uri)).join(', ')
            });
        }

        const selected = await vscode.window.showQuickPick(items, {
            placeHolder: t('All codeowners'),
            canPickMany: false
        });

        if (selected) {
            const owners = selected.label.replace('$(organization) ', '');
            const files = codeownerMap.get(owners);

            if (files && files.length > 0) {
                const fileItems = files.map(uri => ({
                    label: vscode.workspace.asRelativePath(uri),
                    uri
                }));

                const selectedFile = await vscode.window.showQuickPick(fileItems, {
                    placeHolder: `Files owned by ${owners}`
                });

                if (selectedFile) {
                    const document = await vscode.workspace.openTextDocument(selectedFile.uri);
                    await vscode.window.showTextDocument(document);
                }
            }
        }
    }

    public refreshCodeowners(): void {
        this.serviceManager.refreshAllServices();
        vscode.window.showInformationMessage('Codeowners refreshed');
    }
}
