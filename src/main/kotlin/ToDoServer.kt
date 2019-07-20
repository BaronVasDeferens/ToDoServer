import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import kotlin.concurrent.thread

object ToDoServer {

    private const val sendPort = 12321
    private const val receivePort = 12322

    // ExpirationMillis: if the difference between completedMillis and now() is greater than
    // this value, an item should be deleted
    private const val expirationMillis = 1 * 60 * 1000


    var toDoItems: List<ToDoItem> = listOf()

    // TODO: move into android project as separate module

    @JvmStatic
    fun main(args: Array<String>) {

        thread(start = true) {
            val ip = InetAddress.getLocalHost().hostAddress.toString()
            println(">>> Starting $ip...")
            val serverSocket = ServerSocket(sendPort)
            while (true) {
                val sendSocket = serverSocket.accept()
                println(">>> SENDING TO (${sendSocket.remoteSocketAddress}) :: ${toDoItems.size} ITEMS")

                synchronized(toDoItems) {

                    toDoItems = toDoItems.filter {(it.completedMillis == 0L) || (it.completedMillis + expirationMillis > System.currentTimeMillis())}

                    val outputStream = sendSocket.getOutputStream()
                    val payloadContent = Json.stringify(ToDoItem.serializer().list, toDoItems)
                    outputStream.write(payloadContent.toByteArray())
                    sendSocket.close()
                }
            }
        }

        thread(start = true) {

            val serverSocket = ServerSocket(receivePort)
            while (true) {
                val receiveSocket = serverSocket.accept()

                val dataStream = receiveSocket.getInputStream()
                val payloadAsBytes = dataStream.readBytes().toString(Charset.defaultCharset())
                synchronized(toDoItems) {
                    println(">>> Data IN (${receiveSocket.remoteSocketAddress})")
                    val candidateItems = Json.parse(ToDoItem.serializer().list, payloadAsBytes)
                    val canonicalItems: MutableList<ToDoItem> = mutableListOf()

                    candidateItems.forEach { newItem ->
                        val existingItem: ToDoItem? = toDoItems.firstOrNull{ it.taskId == newItem.taskId }

                        println(existingItem)

                        when {
                            // No record found for a task with that id: add as new item
                            existingItem == null -> {
                                println(">>> \tNEW TASK: ${newItem.taskName}")
                                canonicalItems.add(newItem)
                            }
                            // Record of existing item found: should it be updated?
                            existingItem.lastModifiedMillis < newItem.lastModifiedMillis -> {
                                println(">>> \tUPDATING TASK: ${newItem.taskName}")
                                canonicalItems.add(newItem)
                            }
                            // Just
                            else -> canonicalItems.add(existingItem)
                        }
                    }
                    toDoItems = canonicalItems
                }
                receiveSocket.close()
            }
        }

    }

}

