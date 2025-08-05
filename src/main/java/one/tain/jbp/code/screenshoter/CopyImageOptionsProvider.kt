package one.tain.jbp.code.screenshoter

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

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
        val scale: Double = 4.0,
        val removeCaret: Boolean = true,
        val chopIndentation: Boolean = true,
        val directoryToSave: String? = null,
        val padding: Int = 0,
        val format: TransferableImage.Format = TransferableImage.Format.PNG
    )
}
