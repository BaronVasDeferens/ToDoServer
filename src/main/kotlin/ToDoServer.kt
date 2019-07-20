import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.Charset
import kotlin.concurrent.thread

object ToDoServer {

    val sendPort = 12321
    val receivePort = 12322


    var toDoItems: List<ToDoItem> = listOf(


//
//        ToDoItem(
//            "b263dba0-39fe-4c7e-9d3f-3f705031347f",
//            "Write A Task Management App",
//            "I'm so proud of you!",
//            ToDoItem.TaskType.TASK,
//            ToDoItem.TaskUrgency.MEDIUM,
//            System.currentTimeMillis()
//        )
//        , ToDoItem(
//            "f420942d-5c65-46b2-971d-e9cd1c66fb02",
//            "Buy fruit",
//            "Organic, please!",
//            ToDoItem.TaskType.SHOPPING,
//            ToDoItem.TaskUrgency.LOW,
//            System.currentTimeMillis()
//        ),
//
//        ToDoItem(
//            "94747e0b-8466-456d-9355-bb32f930c36a",
//            "Taxes",
//            "Do your taxes, foo",
//            ToDoItem.TaskType.TASK,
//            ToDoItem.TaskUrgency.HIGH,
//            System.currentTimeMillis()
//        ),
//
//        ToDoItem(
//            "c8fb1ae0-1db8-498b-8b6e-47d5ddcecdfb",
//            "Sell Mustang",
//            "Find that guy's number",
//            ToDoItem.TaskType.TASK,
//            ToDoItem.TaskUrgency.HIGH,
//            System.currentTimeMillis()
//        )
    )

    // TODO: move into android project as separate module

    @JvmStatic
    fun main(args: Array<String>) {

        thread(start = true) {
            val ip = InetAddress.getLocalHost().hostAddress.toString()
            println(">>> Starting $ip...")
            val serverSocket = ServerSocket(sendPort)
            while (true) {
                val sendSocket = serverSocket.accept()
                println(">>> Data OUT (${sendSocket.remoteSocketAddress}) :: SENDING ${toDoItems.size} ITEMS")

                synchronized(toDoItems) {
                    val outputStream = sendSocket.getOutputStream()
//                    toDoItems.forEach { println(it) }
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

