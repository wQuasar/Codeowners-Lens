export interface CodeOwnerRule {
    pattern: string;
    owners: string[];
    lineNumber: number;
}

export function parseCodeOwnerLine(lineNumber: number, lineInfo: string[]): CodeOwnerRule | null {
    if (lineInfo.length < 2) {
        return null;
    }
    return {
        pattern: lineInfo[0],
        owners: lineInfo.slice(1).sort(),
        lineNumber
    };
}
