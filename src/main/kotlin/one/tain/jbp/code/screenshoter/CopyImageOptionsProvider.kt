package one.tain.jbp.code.screenshoter

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Service class for managing and persisting user options for the code screenshoter plugin.
 * This class handles loading and saving plugin settings to the project workspace file.
 */
@Service(Service.Level.PROJECT)
@State(name = "CopyImageOptionsProvider", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CopyImageOptionsProvider : PersistentStateComponent<CopyImageOptionsProvider.State> {
    companion object {
        /**
         * Gets the singleton instance of the CopyImageOptionsProvider for a given project.
         * This method provides access to the project-specific settings.
         *
         * @param project The project for which to get the options provider
         * @return The CopyImageOptionsProvider instance for the specified project
         */
        fun getInstance(project: Project): CopyImageOptionsProvider {
            return project.getService(CopyImageOptionsProvider::class.java)
        }
        var defaultState = State()
    }

    /**
     * Gets the current state of the plugin options.
     * This method is called by the IntelliJ platform to retrieve the current settings
     * for persistence.
     *
     * @return The current State object containing all user options
     */
    override fun getState(): State {
        return defaultState
    }

    /**
     * Loads the plugin options from persisted state.
     * This method is called by the IntelliJ platform when loading saved settings.
     *
     * @param state The state object to load into the provider
     */
    override fun loadState(state: State) {
        defaultState = state
    }

    /**
     * Data class representing the state of all plugin options that can be persisted.
     * This includes image formatting options, saving preferences, and other user settings.
     *
     * @property scale The image scale factor to apply when creating screenshots (default is 4.0)
     * @property removeCaret Whether to hide the caret in the generated image (default is true)
     * @property chopIndentation Whether to remove leading indentation from code lines (default is true)
     * @property directoryToSave The directory path where images should be saved (default is system Pictures directory)
     * @property padding The amount of padding to add around the image content (default is 0)
     * @property format The output format for images (PNG or SVG, default is PNG)
     * @property dateTimePattern The date/time pattern used for generating unique filenames (default is yyyyMMdd_HHmmss)
     */
    data class State(
        /** Image scale factor, default is 4.0 */
        val scale: Double = 4.0,
        /** Whether to remove caret, default is true */
        val removeCaret: Boolean = true,
        /** Whether to chop indentation, default is true */
        val chopIndentation: Boolean = true,
        /** Directory path to save images, can be null */
        val directoryToSave: String = ScreenShoterUtils.pictureDefaultDirectory(),
        /** Image padding, default is 0 */
        val padding: Int = 0,
        /** Image format, default is PNG */
        val format: Format = Format.PNG,
        /** line limit in bytes for warning dialog, default is 50 */
        val lineLimitToWarn: Long = 50L,
        /** Date time pattern for file naming, default is yyyyMMdd_HHmmss */
        val dateTimePattern: String = "yyyyMMdd_HHmmss",
        /** Whether gutter icons are shown, default is false */
        val gutterIconsShown: Boolean = false,
        /** Whether folding outline is shown, default is false */
        val foldingOutlineShown: Boolean = false,
        /** Whether inner whitespace is shown, default is false */
        val innerWhitespaceShown : Boolean = false,
        /** Whether indent guides are shown, default is false */
        val indentGuidesShown : Boolean = false,
        /** Whether line numbers are shown, default is false */
        val lineNumbersShown : Boolean = false,
        )
}
