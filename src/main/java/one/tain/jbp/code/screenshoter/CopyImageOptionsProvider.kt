package one.tain.jbp.code.screenshoter

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import java.util.*

@State(name = "CopyImageOptionsProvider", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CopyImageOptionsProvider : PersistentStateComponent<CopyImageOptionsProvider.State> {
    private val myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState.myScale = state.myScale
        myState.myRemoveCaret = state.myRemoveCaret
        myState.myChopIndentation = state.myChopIndentation
        myState.myDirectoryToSave = state.myDirectoryToSave
        myState.myPadding = state.myPadding
        myState.myFormat = state.myFormat
    }

    class State {
        @JvmField
        var myScale: Double = 4.0
        @JvmField
        var myRemoveCaret: Boolean = true
        @JvmField
        var myChopIndentation: Boolean = true
        @JvmField
        var myDirectoryToSave: String? = null
        @JvmField
        var myPadding: Int = 0
        @JvmField
        var myFormat: TransferableImage.Format? = TransferableImage.Format.PNG

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val state = other as State
            return state.myScale.compareTo(myScale) == 0 && myRemoveCaret == state.myRemoveCaret && myChopIndentation == state.myChopIndentation && myPadding == state.myPadding && myFormat === state.myFormat &&
                    myDirectoryToSave == state.myDirectoryToSave
        }

        override fun hashCode(): Int {
            return Objects.hash(myScale, myRemoveCaret, myChopIndentation, myDirectoryToSave, myPadding, myFormat)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CopyImageOptionsProvider {
            return project.getService(CopyImageOptionsProvider::class.java)
        }
    }
}
