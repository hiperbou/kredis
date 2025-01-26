package main

import org.slf4j.LoggerFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

sealed class Operation(val key: String, val clientChannel: Channel<String> = Channel(RENDEZVOUS))
class GetOperation(key: String) : Operation(key)
class SetOperation(key: String, val value: String) : Operation(key)
class DelOperation(key: String ) : Operation(key)
class IncrOperation(key: String) : Operation(key)
class DecrOperation(key: String) : Operation(key)

enum class Command(
    val key: Boolean = false,
    val value: Boolean = false,
    val operation: (String?, String?) -> Operation
){
    STOP(false, false, { _,_ -> throw StopCommandThrowable() }),
    GET(true, false, { k,_ -> GetOperation(k!!) }),
    SET(true, true, { k,v -> SetOperation(k!!, v!!) }),
    DEL(true, false, { k,_ -> DelOperation(k!!) }),
    INCR(true, false, { k,_ -> IncrOperation(k!!) }),
    DECR(true, false, { k,_ -> DecrOperation(k!!) });

    fun assertParams(k:String?, v:String?) {
        if (key && k == null && value && v == null) throw InvalidParamsException("Missing key and value param")
        if (key && k == null) throw InvalidParamsException("Missing key param")
        if (value && v == null) throw InvalidParamsException("Missing value param")
    }
}

class StopCommandThrowable: Throwable()
class NoInputException: Exception()
class InvalidParamsException(message:String): Exception(message)

val log = LoggerFactory.getLogger("KRedis")

fun main():Unit = runBlocking {
    val operationChannel = Channel<Operation>(UNLIMITED)
    val state = mutableMapOf<String, String>()

    launch {
        while(true) {
            with(operationChannel.receive()) {
                when (this) {
                    is GetOperation -> state[key] ?: ""
                    is SetOperation -> { state[key] = value; "OK" }
                    is DelOperation -> if (state.remove(key) == null) "0" else "1"
                    is IncrOperation, is DecrOperation -> when(val intValue = (state[key] ?: "0").toIntOrNull()) {
                        null -> "Error: value is not an integer or out of range"
                        else -> ((intValue + if(this is IncrOperation) 1 else -1).toString()).also { state[key] = it }
                    }
                }.let { clientChannel.send(it) }
                clientChannel.close()
            }
        }
    }
    runServer(operationChannel)
}

suspend fun runServer(operationChannel: Channel<Operation>) = coroutineScope {
    launch(Dispatchers.IO) {
        val port = System.getenv("PORT")?.toInt() ?: 9669
        val server = ServerSocket(port)
        log.info("Listening on port $port")

        while(true) {
            val client = server.accept()
            log.info("Starting handler for: $client")
            launch(Dispatchers.IO) {
                try {
                    val output = PrintWriter(client.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(client.inputStream))
                    handleClient(output, reader, operationChannel)
                } catch (t: StopCommandThrowable) {
                    log.info("Closing client $client by request ")
                } catch (t: NoInputException) {
                    log.warn("Received no input from $client")
                } catch (t: Throwable) {
                    log.error("Error handling client", t)
                } finally {
                    client.close()
                }
            }
        }
    }
}

suspend fun handleClient(output:PrintWriter, reader:BufferedReader, operationChannel: Channel<Operation>) {
    withContext(Dispatchers.IO) {
        while (true) {
            val input = reader.readLine() ?: throw NoInputException()
            //println("received: $input")
            val tokens = input.trim().split(" ")
            val key = tokens.getOrNull(1)
            val value = tokens.getOrNull(2)

            val command = try {
                Command.valueOf(tokens.first().toUpperCase()).apply { assertParams(key, value) }
            } catch (e:InvalidParamsException) {
                output.println(e.message)
                continue
            } catch (e:Throwable) {
                output.println("Error: Invalid command: $input")
                continue
            }

            val operation = command.operation(key, value)
            operationChannel.send(operation)
            output.println(operation.clientChannel.receive())
        }
    }
}