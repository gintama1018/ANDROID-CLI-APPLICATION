package com.gintama.nlcli.executor

import com.gintama.nlcli.model.Command
import com.gintama.nlcli.model.ExecutionResult

interface ICommandExecutor {
    suspend fun execute(command: Command): ExecutionResult
}
