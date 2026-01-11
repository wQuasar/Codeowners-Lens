import { CodeOwnerRule } from './codeOwnerRule';

export interface Segment {
    regex: RegExp;
    optional: boolean;
}

export class RuleGlob {
    public readonly codeOwnerRule: CodeOwnerRule;
    public readonly segments: Segment[];

    constructor(codeOwnerRule: CodeOwnerRule) {
        this.codeOwnerRule = codeOwnerRule;

        let pattern = codeOwnerRule.pattern
            .replace(/^!/, '') // Remove negation prefix
            .replace(/\/$/, ''); // Remove trailing slash

        // Transform pattern according to GitHub CODEOWNERS rules
        // *.js -> **/*.js (file patterns without / should match anywhere)
        // /path/** -> /path/** (already has **)
        // /path/file -> /path/file/** (directory patterns need /**)
        if (pattern.startsWith('*') && !pattern.includes('/')) {
            // File pattern like *.js -> **/*.js
            pattern = `**/${pattern}`;
        } else if (!pattern.endsWith('*')) {
            // Pattern doesn't end with wildcard, so it could match children
            // /docs or /src/api -> /docs/** or /src/api/**
            pattern = `${pattern}/**`;
        }
        // else: pattern already ends with * (like /src/**), leave as-is

        // Collapse **/** (if any)
        pattern = pattern
            .replace(/(\/*\*){2,}\//g, '/**/')
            .replace(/\*\*\/\*\*\//g, '**/')
            .replace(/\/\*\*\/\*\*$/g, '/**');

        // Add wildcard for directories with no slash prefix
        // Leading / means "from root", so remove it and don't add **
        let expectedPath: string;
        if (pattern.startsWith('/')) {
            expectedPath = pattern.substring(1); // Remove leading /
        } else {
            expectedPath = `**/${pattern}`; // Match anywhere
        }

        this.segments = expectedPath.split('/').map(seg => {
            if (seg === '**') {
                return { regex: new RegExp('.*'), optional: true };
            } else {
                // Escape special regex characters except *
                const escaped = seg.replace(/([^a-zA-Z0-9 *])/g, '\\$1').replace(/\*/g, '.*');
                return { regex: new RegExp(`^${escaped}$`), optional: false };
            }
        });
    }

    public matches(relativePath: string): boolean {
        const isNegated = this.codeOwnerRule.pattern.startsWith('!');
        const pathMatches = this.matchSegments(relativePath.split('/'));
        return isNegated ? !pathMatches : pathMatches;
    }

    private matchSegments(paths: string[]): boolean {
        const sizeOfSegments = this.segments.length;
        let match = new Set<number>([0]);
        let nextMatch = new Set<number>();

        for (const path of paths) {
            // Process all segment indices, including ones added during iteration
            const processed = new Set<number>();

            while (true) {
                // Find an unprocessed segment index
                let segmentIndex = -1;
                for (const idx of match) {
                    if (!processed.has(idx)) {
                        segmentIndex = idx;
                        break;
                    }
                }

                if (segmentIndex === -1) {
                    // All segments processed
                    break;
                }

                processed.add(segmentIndex);

                if (segmentIndex === sizeOfSegments) {
                    continue;
                }

                const segment = this.segments[segmentIndex];
                if (segment.regex.test(path)) {
                    nextMatch.add(segmentIndex + 1);
                }

                if (segment.optional) {
                    nextMatch.add(segmentIndex);
                    match.add(segmentIndex + 1); // Will be processed in next iteration of while
                }
            }

            if (nextMatch.size === 0) {
                return false;
            }

            const temp = match;
            match = nextMatch;
            nextMatch = temp;
            nextMatch.clear();
        }

        return match.has(sizeOfSegments) ||
               (match.has(sizeOfSegments - 1) && this.segments[sizeOfSegments - 1].optional);
    }
}
