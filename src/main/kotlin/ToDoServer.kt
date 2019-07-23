import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object ToDoServer {

    private const val sendPort = 12321
    private const val receivePort = 12322



    // ExpirationMillis: if the difference between completedMillis and now() is greater than
    // this value, an item should be deleted
    private const val expirationSeconds = 10
    private const val expirationMillis = expirationSeconds * 1000


    private val toDoItems = ConcurrentHashMap<String, ToDoItem>()

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

                    toDoItems.values.forEach{item ->
                        if (item.completedMillis > 0L && item.completedMillis + expirationMillis < System.currentTimeMillis()) {
                            toDoItems.remove(item.taskId)
                        }
                    }

                    val outputStream = sendSocket.getOutputStream()
                    val payloadContent = Json.stringify(ToDoItem.serializer().list, toDoItems.values.toList())
                    outputStream.write(payloadContent.toByteArray())
                    sendSocket.close()
                }
            }
        }

        thread(start = true) {

            /**
             * Changes to an item are made on a device and are immediately sent to this thread
             */

            val serverSocket = ServerSocket(receivePort)
            while (true) {
                val receiveSocket = serverSocket.accept()

                val dataStream = receiveSocket.getInputStream()
                val payloadAsBytes = dataStream.readBytes().toString(Charset.defaultCharset())
                synchronized(toDoItems) {
                    println(">>> Data IN (${receiveSocket.remoteSocketAddress})")

                    val candidateItems = Json.parse(ToDoItem.serializer().list, payloadAsBytes)

                    candidateItems
                        .filter {newItem ->
                            newItem.completedMillis == 0L || newItem.completedMillis + expirationMillis > System.currentTimeMillis()
                        }
                        .forEach { newItem ->
                        val existingItem: ToDoItem? = toDoItems[newItem.taskId]

                        when {
                            // No record found for a task with that id: add as new item
                            existingItem == null -> {
                                println(">>> \tNEW TASK: $newItem")
                                toDoItems[newItem.taskId] = newItem
                            }
                            // Record of existing item found: update if newer
                            newItem.lastModifiedMillis > existingItem.lastModifiedMillis -> {
                                println(">>> \tUPDATING TASK: $newItem")
                               toDoItems[newItem.taskId] = newItem
                            }
                        }
                    }
                }
                receiveSocket.close()
            }
        }

    }

}

