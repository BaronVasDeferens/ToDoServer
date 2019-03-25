import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.*

object ToDoServer {

    val port = 12321

    private val defaultItem = ToDoItem(
        UUID.randomUUID().toString(),
        "Write A Task Management App",
        "I'm so proud of you!",
        ToDoItem.TaskType.TASK,
        ToDoItem.TaskUrgency.MEDIUM,
        System.currentTimeMillis()
    )

    private val secondItem = ToDoItem(
        UUID.randomUUID().toString(),
        "Buy fruit",
        "Organic, please!",
        ToDoItem.TaskType.SHOPPING,
        ToDoItem.TaskUrgency.LOW,
        System.currentTimeMillis()
    )

    private val thirdItem = ToDoItem(
        UUID.randomUUID().toString(),
        "Taxes",
        "Do your taxes, foo",
        ToDoItem.TaskType.TASK,
        ToDoItem.TaskUrgency.HIGH,
        System.currentTimeMillis()
    )

    val toDoItems = listOf(defaultItem, secondItem, thirdItem)


    @JvmStatic
    fun main(args: Array<String>) {


        val serverSocket = ServerSocket(port)
        println("Listening on $port...")



        while (true) {
            val connectionSocket = serverSocket.accept()
            println("CONNECTION...")
            //receiveData(connectionSocket)
            sendData(connectionSocket)
            connectionSocket.close()
        }
    }


    private fun receiveData(socket: Socket): String {
        val dataStream = socket.getInputStream()
        val payloadAsBytes = dataStream.readAllBytes()
        return payloadAsBytes.toString(Charset.defaultCharset())
    }

    private fun sendData(socket: Socket) {
        val outputStream = socket.getOutputStream()


        toDoItems.forEach { println(it) }
        val payloadContent = Json.stringify(ToDoItem.serializer().list, toDoItems)
        outputStream.write(payloadContent.toByteArray())
    }
}

