package ru.lisss79.tinkofftradingrobot.queries_and_responses

enum class ExecutionReportStatus(val rus_name: String, val final: Boolean) {
    EXECUTION_REPORT_STATUS_UNSPECIFIED("не определен", false),
    EXECUTION_REPORT_STATUS_FILL("выполнена", true),
    EXECUTION_REPORT_STATUS_REJECTED("отклонена", true),
    EXECUTION_REPORT_STATUS_CANCELLED("отменена", true),
    EXECUTION_REPORT_STATUS_NEW("новая", false),
    EXECUTION_REPORT_STATUS_PARTIALLYFILL("частично выполнена", false);

    companion object {
        fun parse(response: String): ExecutionReportStatus {
            for (status in ExecutionReportStatus.values()) {
                if (status.name == response) return status
            }
            return EXECUTION_REPORT_STATUS_UNSPECIFIED
        }
    }
}