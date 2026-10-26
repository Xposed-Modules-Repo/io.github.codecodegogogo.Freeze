package com.aistra.hail.ui.home

internal fun hasTagNameConflict(
    tags: List<Pair<String, Int>>,
    name: String,
    candidateId: Int,
    excludedTagId: Int? = null,
): Boolean = tags.any { (existingName, existingId) ->
    existingId != excludedTagId &&
        (existingName == name || existingId == candidateId)
}
