package com.wquasar.codeowners.lens.glob

import java.util.*
import javax.inject.Inject

internal class RuleGlobMatcher @Inject constructor() {
    fun matches(ruleGlob: RuleGlob, relativePath: String): Boolean =
        matchSegments(ruleGlob.segments, relativePath.split('/')) != ruleGlob.codeOwnerRule.pattern.startsWith("!")

    private fun matchSegments(segments: List<RuleGlob.Segment>, paths: List<String>): Boolean {
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
