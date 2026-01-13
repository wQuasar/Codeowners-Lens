import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

class LocalizationService {
    private translations: Map<string, string> = new Map();
    private locale: string;

    constructor(extensionPath: string) {
        this.locale = vscode.env.language;
        this.loadTranslations(extensionPath);
    }

    private loadTranslations(extensionPath: string): void {
        try {
            // Try to load locale-specific bundle
            const bundlePath = path.join(extensionPath, 'l10n', `bundle.l10n.${this.locale}.json`);
            if (fs.existsSync(bundlePath)) {
                const content = fs.readFileSync(bundlePath, 'utf-8');
                const translations = JSON.parse(content);
                Object.entries(translations).forEach(([key, value]) => {
                    this.translations.set(key, value as string);
                });
                console.log(`[CodeOwners] Loaded translations for locale: ${this.locale}`);
            } else {
                console.log(`[CodeOwners] No translations found for locale: ${this.locale}, using English`);
            }
        } catch (error) {
            console.error('[CodeOwners] Error loading translations:', error);
        }
    }

    public t(key: string, ...args: any[]): string {
        let translation = this.translations.get(key) || key;

        // Replace placeholders {0}, {1}, etc.
        args.forEach((arg, index) => {
            translation = translation.replace(`{${index}}`, String(arg));
        });

        return translation;
    }
}

let localizationService: LocalizationService | null = null;

export function initializeLocalization(extensionPath: string): void {
    localizationService = new LocalizationService(extensionPath);
}

export function t(key: string, ...args: any[]): string {
    if (!localizationService) {
        console.warn('[CodeOwners] Localization not initialized, returning key');
        return key;
    }
    return localizationService.t(key, ...args);
}
