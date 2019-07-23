
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ToDoItem(
    val taskId: String = UUID.randomUUID().toString(),
    val taskName: String,
    val taskDetail: String,
    val taskType: TaskType = TaskType.TASK,
    val taskUrgency: TaskUrgency = TaskUrgency.MEDIUM,
    val createdMillis: Long,
    val completedMillis: Long = 0L,
    val lastModifiedMillis: Long = 0L
) {

    enum class TaskType {
        TASK,
        SHOPPING
    }

    enum class TaskUrgency {
        LOW,
        MEDIUM,
        HIGH
    }

    override fun toString(): String {
        return "$taskName ($taskType $taskUrgency) lastModified: $lastModifiedMillis completed: $completedMillis"
    }
}