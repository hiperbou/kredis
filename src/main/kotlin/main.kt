package main

import org.slf4j.LoggerFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.io.*
import java.net.*

sealed class Operation(val key: String, open val value:String? = null, val clientChannel: Channel<String> = Channel(RENDEZVOUS))
class GetOperation(key: String) : Operation(key)
class SetOperation(key: String, override val value: String) : Operation(key)
class DelOperation(key: String) : Operation(key)
class IncrOperation(key: String, value:String?) : Operation(key, value)
class DecrOperation(key: String, value:String?) : Operation(key, value)

enum class Command(
    private val keyRequired: Boolean = false,
    private val valueRequired: Boolean = false,
    val operation: (String?, String?) -> Operation
){
    STOP(false, false, { _,_ -> throw StopCommandThrowable() }),
    GET(true, false, { k,_ -> GetOperation(k!!) }),
    SET(true, true, { k,v -> SetOperation(k!!, v!!) }),
    DEL(true, false, { k,_ -> DelOperation(k!!) }),
    INCR(true, false, { k,v -> IncrOperation(k!!, v) }),
    DECR(true, false, { k,v -> DecrOperation(k!!, v) });

    fun validateParams(k:String?, v:String?) {
        if (keyRequired && k == null && valueRequired && v == null) throw InvalidParamsException("Error: Missing key and value param")
        if (keyRequired && k == null) throw InvalidParamsException("Error: Missing key param")
        if (valueRequired && v == null) throw InvalidParamsException("Error: Missing value param")
    }
}

class StopCommandThrowable: Throwable()
class NoInputException: Exception()
class InvalidParamsException(message:String): Exception(message)

val log = LoggerFactory.getLogger("KRedis")
const val DEFAULT_PORT = 9669

fun main(args: Array<String>):Unit = runBlocking {
    val operationChannel = Channel<Operation>(UNLIMITED)
    val state = mutableMapOf<String, String>()

    launch {
        for(operation in operationChannel) with(operation) {
            when (this) {
                is GetOperation -> state[key] ?: ""
                is SetOperation -> { state[key] = value; "OK" }
                is DelOperation -> if (state.remove(key) == null) "0" else "1"
                is IncrOperation, is DecrOperation -> when(val intValue = (state[key] ?: "0").toIntOrNull()) {
                    null -> "Error: key '$key' value is not an integer or out of range"
                    else -> value?.toIntOrNull().let { increment ->
                        when {
                            (value != null && increment == null) -> "Error: provided value '$value' is not an integer or out of range"
                            else -> ((intValue + (increment ?: 1) * if (this is IncrOperation) 1 else -1).toString()).also { state[key] = it }
                        }
                    }
                }
            }.let { clientChannel.send(it) }
            clientChannel.close()
        }
    }
    runServer(args.getOrNull(0)?.toIntOrNull() ?: System.getenv("PORT")?.toInt() ?: DEFAULT_PORT, operationChannel)
}

suspend fun runServer(port:Int, operationChannel: Channel<Operation>) = coroutineScope {
    launch(Dispatchers.IO) {
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
                } catch (t: Throwable) {
                    when(t){
                        is StopCommandThrowable -> log.info("Closing client $client by request ")
                        is SocketException -> log.info("Closing client $client by connection reset")
                        is NoInputException -> log.warn("Received no input from $client")
                        else -> log.error("Error handling client", t)
                    }
                } finally {
                    client.close()
                }
            }
        }
    }
}

suspend fun handleClient(output:PrintWriter, reader:BufferedReader, operationChannel: Channel<Operation>) = withContext(Dispatchers.IO) {
    while (true) {
        val input = reader.readLine() ?: throw NoInputException()
        //println("received: $input")
        val tokens = input.trim().split("\\s+".toRegex(), limit = 3)
        val key = tokens.getOrNull(1)
        val value = tokens.getOrNull(2)

        val command = try {
            Command.valueOf(tokens.first().uppercase()).apply { validateParams(key, value) }
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