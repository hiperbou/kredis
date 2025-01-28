package main

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.*
import java.net.*

object SocketClient {
    private val log = LoggerFactory.getLogger("SocketClient")
    private const val DEFAULT_PORT = 9669

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val host = args.getOrNull(0) ?: "localhost"
        val port = args.getOrNull(1)?.toIntOrNull() ?: DEFAULT_PORT

        try {
            Socket(host, port).use { socket ->
                log.info("Connected to server")
                log.info("Usage: STOP, GET key, SET key value, DEL key, INCR key, DECR key")

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                BufferedReader(InputStreamReader(System.`in`)).use { consoleReader ->
                    while (true) {
                        val input = consoleReader.readLine()
                        if (input == null) {
                            log.warn("Closing connection...")
                            break
                        }
                        writer.println(input)

                        val data = reader.readLine()
                        if (data == null) {
                            log.warn("Connection closed by server")
                            break
                        }
                        log.info("Received: $data")
                    }
                }
            }
        } catch (t: SocketException) {
            log.info(t.message)
        } catch (t: Throwable) {
            log.error("Error: ", t)
        }
    }
}