package com.gintama.nlcli.model

enum class AppType(val rawValue: String) {
    WHATSAPP("whatsapp"),
    SMS("sms"),
    PHONE("phone"),
    SYSTEM("system"),
    UTILITY("utility"),
    MEDIA("media"),
    CLOCK("clock"),
    NOTES("notes"),
    TODOS("todos");

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
    DRY_RUN("dry_run"),
    TORCH("torch"),
    VOLUME("volume"),
    BATTERY("battery"),
    STORAGE("storage"),
    DEVICE_INFO("device_info"),
    CALC("calc"),
    CONVERT("convert"),
    NOTE("note"),
    TODO("todo"),
    DEV_TOOL("dev_tool"),
    ALARM("alarm"),
    TIMER("timer"),
    MEDIA("media"),
    SNIPPET("snippet"),
    MACRO("macro");

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
