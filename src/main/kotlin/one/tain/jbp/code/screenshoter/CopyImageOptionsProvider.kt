package one.tain.jbp.code.screenshoter

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.time.format.DateTimeFormatter

@Service(Service.Level.PROJECT)
@State(name = "CopyImageOptionsProvider", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CopyImageOptionsProvider : PersistentStateComponent<CopyImageOptionsProvider.State> {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): CopyImageOptionsProvider {
            return project.getService(CopyImageOptionsProvider::class.java)
        }
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    data class State(
        /** Image scale factor, default is 4.0 */
        val scale: Double = 4.0,
        /** Whether to remove caret, default is true */
        val removeCaret: Boolean = true,
        /** Whether to chop indentation, default is true */
        val chopIndentation: Boolean = true,
        /** Directory path to save images, can be null */
        val directoryToSave: String? = null,
        /** Image padding, default is 0 */
        val padding: Int = 0,
        /** Image format, default is PNG */
        val format: Format = Format.PNG,
        /** Size limit in bytes for warning dialog, default is 3MB */
        val sizeLimitToWarn: Long = 3000000L,
        /** Date time pattern for file naming, default is yyyyMMdd_HHmmss */
        val dateTimePattern: String = "yyyyMMdd_HHmmss"
    )
}

fun getDateTimePattern(project: com.intellij.openapi.project.Project?): DateTimeFormatter {
    val options = project?.let { CopyImageOptionsProvider.getInstance(it).state }
    val pattern = options?.dateTimePattern ?: "yyyyMMdd_HHmmss"
    return DateTimeFormatter.ofPattern(pattern)
}

fun getSizeLimitToWarn(project: com.intellij.openapi.project.Project?): Long {
    val options = project?.let { CopyImageOptionsProvider.getInstance(it).state }
    return options?.sizeLimitToWarn ?: 3000000L
}
