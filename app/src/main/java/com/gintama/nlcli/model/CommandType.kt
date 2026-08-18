package com.gintama.nlcli.model

enum class AppType(val rawValue: String) {
    WHATSAPP("whatsapp"),
    SMS("sms"),
    PHONE("phone"),
    SYSTEM("system"),
    UTILITY("utility");

    companion object {
        fun fromString(value: String): AppType {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: UTILITY
        }
    }
}

enum class ActionType(val rawValue: String) {
    SEND_MESSAGE("send_message"),
    CALL("call"),
    OPEN_APP("open_app"),
    SEARCH("search"),
    HELP("help"),
    CLEAR("clear"),
    HISTORY("history"),
    STATUS("status"),
    DRY_RUN("dry_run");

    companion object {
        fun fromString(value: String): ActionType {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) } ?: HELP
        }
    }
}

enum class ParseSource {
    REGEX,
    LLM,
    MANUAL
}
