package com.example.damaiassistant.domain

import com.example.damaiassistant.model.TaskConfig

enum class ValidationCode {
    EVENT_REQUIRED,
    PERFORMANCE_REQUIRED,
    RELEASE_TIME_REQUIRED,
    RELEASE_IN_PAST,
    TICKET_COUNT_INVALID,
    VIEWER_COUNT_MISMATCH,
    TIER_REQUIRED
}

data class ValidationError(
    val code: ValidationCode
)

object TaskValidator {
    fun validate(config: TaskConfig, nowEpochMs: Long): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        if (config.eventName.isBlank()) {
            errors += ValidationError(ValidationCode.EVENT_REQUIRED)
        }
        if (config.performanceName.isBlank()) {
            errors += ValidationError(ValidationCode.PERFORMANCE_REQUIRED)
        }
        if (config.releaseAtEpochMs <= 0L) {
            errors += ValidationError(ValidationCode.RELEASE_TIME_REQUIRED)
        } else if (config.releaseAtEpochMs <= nowEpochMs) {
            errors += ValidationError(ValidationCode.RELEASE_IN_PAST)
        }
        if (config.ticketCount <= 0) {
            errors += ValidationError(ValidationCode.TICKET_COUNT_INVALID)
        }
        if (config.viewers.size != config.ticketCount) {
            errors += ValidationError(ValidationCode.VIEWER_COUNT_MISMATCH)
        }
        if (config.ticketPreferences.isEmpty()) {
            errors += ValidationError(ValidationCode.TIER_REQUIRED)
        }

        return errors
    }
}
