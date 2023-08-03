package ru.lisss79.tinkoffapitrading.queries_and_responses

enum class ExecutionReportStatus(val rus_name: String) {
    EXECUTION_REPORT_STATUS_UNSPECIFIED("не определен"),
    EXECUTION_REPORT_STATUS_FILL("выполнена"),
    EXECUTION_REPORT_STATUS_REJECTED("отклонена"),
    EXECUTION_REPORT_STATUS_CANCELLED("отменена"),
    EXECUTION_REPORT_STATUS_NEW("новая"),
    EXECUTION_REPORT_STATUS_PARTIALLYFILL("частично выполнена");

    companion object{
        fun parse(response: String): ExecutionReportStatus {
            for(status in ExecutionReportStatus.values()) {
                if(status.name == response) return status
            }
            return EXECUTION_REPORT_STATUS_UNSPECIFIED
        }
    }
}