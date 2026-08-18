package com.gintama.nlcli.parser

import com.gintama.nlcli.model.ParserResult

interface CommandParser {
    suspend fun parse(input: String): ParserResult
}
