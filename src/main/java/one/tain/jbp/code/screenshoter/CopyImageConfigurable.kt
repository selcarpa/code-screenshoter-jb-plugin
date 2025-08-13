package one.tain.jbp.code.screenshoter

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.util.NotNullProducer
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.SwingHelper
import org.jetbrains.annotations.Nls
import javax.swing.*
import javax.swing.event.ChangeEvent

private const val SLIDER_SCALE = 2.0

class CopyImageConfigurable(private val myProject: Project) : SearchableConfigurable, NoScroll {
    private lateinit var myPanel: CopyImageOptionsPanel

    override fun getDisplayName(): @Nls String {
        return "Copy code as image"
    }

    override fun getId(): String {
        return "screenshoter"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent {
        myPanel = CopyImageOptionsPanel()
        myPanel.init()
        return myPanel.wholePanel
    }

    override fun isModified(): Boolean {
        val provider = myProject.getService(CopyImageOptionsProvider::class.java)
        return provider.state != myPanel.toState()
    }

    override fun apply() {
        val provider = myProject.getService(CopyImageOptionsProvider::class.java)
        provider.loadState(myPanel.toState())
    }

    override fun reset() {
        val provider = myProject.getService(CopyImageOptionsProvider::class.java)
        myPanel.fromState(provider.state)
    }

    override fun disposeUIResources() {
    }

    inner class CopyImageOptionsPanel {
        private lateinit var scale: JTextField
        private lateinit var chopIndentation: JCheckBox
        private lateinit var removeCaret: JCheckBox
        lateinit var wholePanel: JPanel
        private lateinit var slider: JSlider
        private lateinit var saveDirectoryPanel: JPanel
        private lateinit var padding: JTextField
        private lateinit var saveDirectory: TextFieldWithHistoryWithBrowseButton
        private lateinit var format: JComboBox<Format>

        fun toState(): CopyImageOptionsProvider.State {
            return CopyImageOptionsProvider.State(
                scale = scale.text.trim().toDoubleOrNull() ?: 4.0,
                padding = padding.text.trim().toIntOrNull() ?: 0,
                chopIndentation = chopIndentation.isSelected,
                removeCaret = removeCaret.isSelected,
                directoryToSave = saveDirectory.text,
                format = format.selectedItem?.let {
                    it as Format?
                } ?: Format.PNG)
        }

        fun fromState(state: CopyImageOptionsProvider.State) {
            chopIndentation.isSelected = state.chopIndentation
            removeCaret.isSelected = state.removeCaret
            slider.value = (state.scale * SLIDER_SCALE).toInt()
            saveDirectory.text = StringUtil.notNullize(state.directoryToSave)
            padding.text = state.padding.toString()
            format.selectedIndex = state.format.ordinal
        }

        fun init() {
            slider.addChangeListener { _: ChangeEvent ->
                scale.text = (slider.value / SLIDER_SCALE).toString()
            }

            Format.entries.forEach { item ->
                format.addItem(item)
            }
        }

        private fun createUIComponents() {
            val singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val field = SwingHelper.createTextFieldWithHistoryWithBrowseButton(
                myProject,
                "Save to Directory",
                singleFolderDescriptor,
                NotNullProducer { ContainerUtil.emptyList() }
            )
            saveDirectoryPanel = field
            saveDirectory = field
        }
    }
}
