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
        private lateinit var scaleTextField: JTextField
        private lateinit var chopIndentation: JCheckBox
        private lateinit var removeCaret: JCheckBox
        lateinit var wholePanel: JPanel
        private lateinit var scaleSlider: JSlider
        private lateinit var directorySelectionPanel: JPanel
        private lateinit var paddingTextField: JTextField
        private lateinit var saveDirectory: TextFieldWithHistoryWithBrowseButton
        private lateinit var formatComboBox: JComboBox<Format>

        fun toState(): CopyImageOptionsProvider.State {
            return CopyImageOptionsProvider.State(
                scale = scaleTextField.text.trim().toDoubleOrNull() ?: 4.0,
                padding = paddingTextField.text.trim().toIntOrNull() ?: 0,
                chopIndentation = chopIndentation.isSelected,
                removeCaret = removeCaret.isSelected,
                directoryToSave = saveDirectory.text,
                format = formatComboBox.selectedItem?.let {
                    it as Format?
                } ?: Format.PNG)
        }

        fun fromState(state: CopyImageOptionsProvider.State) {
            chopIndentation.isSelected = state.chopIndentation
            removeCaret.isSelected = state.removeCaret
            scaleSlider.value = (state.scale * SLIDER_SCALE).toInt()
            saveDirectory.text = StringUtil.notNullize(state.directoryToSave)
            paddingTextField.text = state.padding.toString()
            formatComboBox.selectedIndex = state.format.ordinal
        }

        fun init() {
            scaleSlider.addChangeListener { _: ChangeEvent ->
                scaleTextField.text = (scaleSlider.value / SLIDER_SCALE).toString()
            }

            Format.entries.forEach { item ->
                formatComboBox.addItem(item)
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
            directorySelectionPanel = field
            saveDirectory = field
        }
    }
}
