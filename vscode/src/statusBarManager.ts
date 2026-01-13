import * as vscode from 'vscode';
import { t } from './localization';
import { CodeOwnerServiceManager } from './codeOwnerServiceManager';
import { FileCodeOwnerStateType } from './codeOwnerService';
import { CodeOwnerRule } from './codeOwnerRule';

export class StatusBarManager {
    private statusBarItem: vscode.StatusBarItem;
    private serviceManager: CodeOwnerServiceManager;

    constructor(serviceManager: CodeOwnerServiceManager) {
        this.serviceManager = serviceManager;
        this.statusBarItem = vscode.window.createStatusBarItem(
            vscode.StatusBarAlignment.Right,
            100
        );
        this.statusBarItem.command = 'codeowners-lens.showCodeownerRule';
    }

    public updateStatusBar(editor: vscode.TextEditor | undefined): void {
        if (!editor) {
            this.statusBarItem.hide();
            return;
        }

        const service = this.serviceManager.getServiceForFile(editor.document.uri);
        if (!service) {
            this.statusBarItem.hide();
            return;
        }

        const fileState = service.getFileCodeOwnerState(editor.document.uri);

        switch (fileState.type) {
            case FileCodeOwnerStateType.RuleFoundInCodeOwnerFile:
                if (fileState.codeOwnerRule) {
                    this.showCodeOwners(fileState.codeOwnerRule);
                }
                break;
            case FileCodeOwnerStateType.NoRuleFoundInCodeOwnerFile:
                this.statusBarItem.text = '$(question) ¯\\_(ツ)_/¯';
                this.statusBarItem.tooltip = t('No codeowners found');
                this.statusBarItem.command = undefined;
                this.statusBarItem.show();
                break;
            case FileCodeOwnerStateType.NoCodeOwnerFileFound:
                this.statusBarItem.hide();
                break;
        }
    }

    private showCodeOwners(rule: CodeOwnerRule): void {
        const owners = rule.owners;

        if (owners.length === 0) {
            this.statusBarItem.text = '$(question) ¯\\_(ツ)_/¯';
            this.statusBarItem.tooltip = t('No codeowners found');
            this.statusBarItem.command = undefined;
        } else if (owners.length === 1) {
            this.statusBarItem.text = `$(shield) ${owners[0]}`;
            this.statusBarItem.tooltip = t('Click to show in CODEOWNERS');
            this.statusBarItem.command = 'codeowners-lens.showCodeownerRule';
        } else if (owners.length === 2) {
            this.statusBarItem.text = `$(shield) ${t('{0} & {1}', owners[0], owners[1])}`;
            this.statusBarItem.tooltip = t('Click to show all codeowners');
            this.statusBarItem.command = 'codeowners-lens.showCodeownerRule';
        } else {
            this.statusBarItem.text = `$(shield) ${t('{0}, {1} & {2} more', owners[0], owners[1], owners.length - 2)}`;
            this.statusBarItem.tooltip = t('Click to show all codeowners');
            this.statusBarItem.command = 'codeowners-lens.showCodeownerRule';
        }

        this.statusBarItem.show();
    }

    public dispose(): void {
        this.statusBarItem.dispose();
    }
}
