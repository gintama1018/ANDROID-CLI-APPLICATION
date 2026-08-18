package com.gintama.nlcli.model

sealed class ParserResult {
    data class Success(val command: Command) : ParserResult()
    data class Ambiguous(val candidates: List<Command>, val reason: String) : ParserResult()
    data class Failure(val reason: String, val suggestion: String? = null) : ParserResult()
}
