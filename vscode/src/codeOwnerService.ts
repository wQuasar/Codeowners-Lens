import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { CodeOwnerRule, parseCodeOwnerLine } from './codeOwnerRule';
import { RuleGlob } from './ruleGlob';

export enum FileCodeOwnerStateType {
    NoCodeOwnerFileFound,
    NoRuleFoundInCodeOwnerFile,
    RuleFoundInCodeOwnerFile
}

export interface FileCodeOwnerState {
    type: FileCodeOwnerStateType;
    codeOwnerRule?: CodeOwnerRule;
}

export class CodeOwnerService {
    private static readonly CODEOWNERS_FILE_NAME = 'CODEOWNERS';
    private static readonly validCodeOwnersPaths = [
        `.github/${CodeOwnerService.CODEOWNERS_FILE_NAME}`,
        CodeOwnerService.CODEOWNERS_FILE_NAME,
        `docs/${CodeOwnerService.CODEOWNERS_FILE_NAME}`
    ];

    private ruleGlobs: RuleGlob[] = [];
    private commonCodeOwnerPrefix = '';
    private codeOwnerFile: string | null = null;
    private workspaceRoot: string | null = null;

    constructor(workspaceRoot: string | null) {
        this.workspaceRoot = workspaceRoot;
        this.updateCodeOwnerRules();
    }

    public getFileCodeOwnerState(fileUri: vscode.Uri): FileCodeOwnerState {
        if (!this.workspaceRoot || this.ruleGlobs.length === 0) {
            return { type: FileCodeOwnerStateType.NoCodeOwnerFileFound };
        }

        const relativePath = path.relative(this.workspaceRoot, fileUri.fsPath);
        console.log(`[CodeOwners] Looking for owner of file: "${relativePath}" (workspace: ${this.workspaceRoot})`);
        const result = this.findRuleInRulesMap(relativePath);
        console.log(`[CodeOwners] Result: ${FileCodeOwnerStateType[result.type]}, rule: ${result.codeOwnerRule?.pattern}`);
        return result;
    }

    private findRuleInRulesMap(relativePath: string): FileCodeOwnerState {
        // Find the last matching rule (GitHub CODEOWNERS uses last match wins)
        let matchedRule: CodeOwnerRule | null = null;

        for (const ruleGlob of this.ruleGlobs) {
            if (ruleGlob.matches(relativePath)) {
                matchedRule = ruleGlob.codeOwnerRule;
            }
        }

        if (matchedRule) {
            return {
                type: FileCodeOwnerStateType.RuleFoundInCodeOwnerFile,
                codeOwnerRule: matchedRule
            };
        }

        return { type: FileCodeOwnerStateType.NoRuleFoundInCodeOwnerFile };
    }

    public getTrueCodeOwner(codeOwnerLabel: string): string {
        return this.commonCodeOwnerPrefix + codeOwnerLabel;
    }

    private updateCodeOwnerRules(): void {
        if (!this.workspaceRoot) {
            return;
        }

        const codeOwnerFile = this.findCodeOwnersFile();
        if (!codeOwnerFile) {
            return;
        }

        this.codeOwnerFile = codeOwnerFile;

        try {
            const content = fs.readFileSync(codeOwnerFile, 'utf-8');
            const lines = content.split('\n');

            const codeOwnerRules: CodeOwnerRule[] = [];

            lines.forEach((line, index) => {
                const trimmedLine = line.trim();
                if (trimmedLine && !trimmedLine.startsWith('#')) {
                    const parts = trimmedLine.split(/\s+/).filter(p => p.length > 0);
                    if (parts.length >= 2) {
                        const rule = parseCodeOwnerLine(index, parts);
                        if (rule) {
                            console.log(`[CodeOwners] Parsed rule: pattern="${rule.pattern}", owners=${JSON.stringify(rule.owners)}`);
                            codeOwnerRules.push(rule);
                        }
                    } else {
                        console.warn(`[CodeOwners] Line ${index} has insufficient parts: "${trimmedLine}"`);
                    }
                }
            });

            // Find common prefix
            const commonPrefix = this.findCommonPrefix(codeOwnerRules);
            this.commonCodeOwnerPrefix = commonPrefix;

            if (commonPrefix) {
                codeOwnerRules.forEach(rule => {
                    rule.owners = rule.owners.map(owner =>
                        owner.startsWith(commonPrefix) ? owner.substring(commonPrefix.length) : owner
                    );
                });
            }

            this.ruleGlobs = codeOwnerRules.map(rule => new RuleGlob(rule));
        } catch (error) {
            console.error('Error reading CODEOWNERS file:', error);
        }
    }

    private findCommonPrefix(rules: CodeOwnerRule[]): string {
        const allOwners = rules.flatMap(rule => rule.owners);
        if (allOwners.length === 0) {
            return '';
        }

        let commonPrefix = allOwners[0];
        for (const owner of allOwners.slice(1)) {
            let i = 0;
            while (i < commonPrefix.length && i < owner.length && commonPrefix[i] === owner[i]) {
                i++;
            }
            commonPrefix = commonPrefix.substring(0, i);
            if (!commonPrefix) {
                return '';
            }
        }

        const lastSlashIndex = commonPrefix.lastIndexOf('/');
        return lastSlashIndex !== -1 ? commonPrefix.substring(0, lastSlashIndex + 1) : '';
    }

    private findCodeOwnersFile(): string | null {
        if (!this.workspaceRoot) {
            return null;
        }

        for (const relativePath of CodeOwnerService.validCodeOwnersPaths) {
            const filePath = path.join(this.workspaceRoot, relativePath);
            if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
                return filePath;
            }
        }

        return null;
    }

    public refreshCodeOwnerRules(): void {
        this.ruleGlobs = [];
        this.updateCodeOwnerRules();
    }

    public getCodeOwnerFile(): string | null {
        return this.codeOwnerFile;
    }

    public getCodeOwnerFileUri(): vscode.Uri | null {
        return this.codeOwnerFile ? vscode.Uri.file(this.codeOwnerFile) : null;
    }
}
