package com.wquasar.codeowners.visibility.glob

import java.util.*
import javax.inject.Inject

internal class GlobMatcher @Inject constructor() {
    fun matches(glob: Glob, absolutePath: String): Boolean =
        matchSegments(glob.segments, absolutePath.split('/')) != glob.codeOwnerRule.pattern.startsWith("!")

    // example:
    // path: b/b/c, glob: **/b/c
    //
    // +---+----+---+---+---+
    // |   | ** | b | c |   |
    // +---+----+---+---+---+
    // |   | x  |   |   |   |
    // | b | x  | x | o |   | <- next possible segment
    // | b | x  | x | x |   |
    // | c | x  | o |   | x |
    // +---+----+---+---+---+
    //
    // time: O(n*m), space: O(m), where n - path.size, m - segments.size
    //
    private fun matchSegments(segments: List<Glob.Segment>, paths: List<String>): Boolean {
        val sizeOfSegments = segments.size
        var match = BitSet(sizeOfSegments + 1).apply { set(0) }
        var nextMatch = BitSet(sizeOfSegments + 1) // next possible segment
        for (path in paths) {
            var segmentIndex = -1
            while (true) {
                segmentIndex = match.nextSetBit(segmentIndex + 1)
                if (segmentIndex == -1 || segmentIndex == sizeOfSegments) {
                    break
                }
                val s = segments[segmentIndex]
                if (s.matches(path)) {
                    nextMatch[segmentIndex + 1] = true
                }
                if (s.optional) {
                    nextMatch[segmentIndex] = true
                    match.set(segmentIndex + 1)
                }
            }
            if (nextMatch.isEmpty) {
                return false
            }
            nextMatch = match.apply { match = nextMatch } // swap
            nextMatch.clear()
        }
        return match[sizeOfSegments] || (match[sizeOfSegments - 1] && segments[sizeOfSegments - 1].optional)
    }
}
