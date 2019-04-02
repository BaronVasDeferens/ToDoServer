import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.ServerSocket
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread

object ToDoServer {

    val port = 12321


    var toDoItems: List<ToDoItem> = listOf(

        ToDoItem(
            UUID.randomUUID().toString(),
            "Write A Task Management App",
            "I'm so proud of you!",
            ToDoItem.TaskType.TASK,
            ToDoItem.TaskUrgency.MEDIUM,
            System.currentTimeMillis()
        )
        , ToDoItem(
            UUID.randomUUID().toString(),
            "Buy fruit",
            "Organic, please!",
            ToDoItem.TaskType.SHOPPING,
            ToDoItem.TaskUrgency.LOW,
            System.currentTimeMillis()
        ),

        ToDoItem(
            UUID.randomUUID().toString(),
            "Taxes",
            "Do your taxes, foo",
            ToDoItem.TaskType.TASK,
            ToDoItem.TaskUrgency.HIGH,
            System.currentTimeMillis()
        ),

        ToDoItem(
            UUID.randomUUID().toString(),
            "Sell Mustang",
            "Find that guy's number",
            ToDoItem.TaskType.TASK,
            ToDoItem.TaskUrgency.HIGH,
            System.currentTimeMillis()
        )
    )


    @JvmStatic
    fun main(args: Array<String>) {


        thread(start = true) {
            println(">>> Starting OUTPUT thread...")
            val serverSocket = ServerSocket(12321)
            while (true) {
                val sendSocket = serverSocket.accept()
                println(">>> CONNECTION: Data OUT")

                synchronized(toDoItems) {
                    val outputStream = sendSocket.getOutputStream()
                    toDoItems.forEach { println(it) }
                    val payloadContent = Json.stringify(ToDoItem.serializer().list, toDoItems)
                    outputStream.write(payloadContent.toByteArray())
                    sendSocket.close()
                }

            }
        }

        thread(start = true) {

            val serverSocket = ServerSocket(12322)
            while (true) {
                val receiveSocket = serverSocket.accept()

                val dataStream = receiveSocket.getInputStream()
                val payloadAsBytes = dataStream.readBytes().toString(Charset.defaultCharset())
                synchronized(toDoItems) {
                    println(">>>> RECEIVING!!!")
                    println(payloadAsBytes)
                    toDoItems = Json.parse(ToDoItem.serializer().list, payloadAsBytes)
                }
                receiveSocket.close()
            }
        }

    }

}

