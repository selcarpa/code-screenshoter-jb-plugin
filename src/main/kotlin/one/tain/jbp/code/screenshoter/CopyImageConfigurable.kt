package one.tain.jbp.code.screenshoter

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.util.NotNullProducer
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import org.jetbrains.annotations.Nls
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.ChangeEvent

/**
 * Configurable class that provides the UI for configuring code screenshoter plugin options.
 * This class implements IntelliJ's SearchableConfigurable interface to integrate the
 * plugin settings into the IDE's settings dialog.
 *
 * @property project The current project for which to manage options
 */
class CopyImageConfigurable(private val project: Project) : SearchableConfigurable, NoScroll {
    private var panel: CopyImageOptionsPanel = CopyImageOptionsPanel(project).also {
        it.init()
    }

    /**
     * Gets the display name for this configurable component.
     * This name is shown in the settings dialog navigation panel.
     *
     * @return The localized display name for the configurable
     */
    override fun getDisplayName(): @Nls String {
        return CodeScreenshoterBundle.message("configurable.display.name")
    }

    /**
     * Gets the unique ID for this configurable component.
     * This ID is used for navigation and identification in the settings system.
     *
     * @return The unique identifier for this configurable
     */
    override fun getId(): String {
        return "one/tain/jbp/code/screenshoter"
    }

    /**
     * Gets the help topic for this configurable component.
     * This is used to link to context-sensitive help (returns null if no help is available).
     *
     * @return The help topic identifier, or null if no help is provided
     */
    override fun getHelpTopic(): String? {
        return null
    }

    /**
     * Creates and returns the UI component representing the configuration panel.
     * This method provides the main UI for the plugin settings.
     *
     * @return The JComponent that contains the configuration UI
     */
    override fun createComponent(): JComponent {
        return panel.wholePanel
    }

    /**
     * Checks if the current values in the UI differ from the stored settings.
     * This method is used to enable/disable the Apply button in the settings dialog.
     *
     * @return True if the settings have been modified, false otherwise
     */
    override fun isModified(): Boolean {
        val provider = CopyImageOptionsProvider.getInstance(project)
        return provider.state != panel.toState()
    }

    /**
     * Applies the current UI values to the persistent settings.
     * This method is called when the user clicks the Apply or OK button in the settings dialog.
     */
    override fun apply() {
        val provider = CopyImageOptionsProvider.getInstance(project)
        provider.loadState(panel.toState())
    }

    /**
     * Resets the UI values to match the stored settings.
     * This method is called when the user clicks the Reset button in the settings dialog.
     */
    override fun reset() {
        val provider = CopyImageOptionsProvider.getInstance(project)
        panel.fromState(provider.state)
    }

    /**
     * Disposes of any UI resources when the configuration dialog is closed.
     * This method handles cleanup of UI resources if needed (currently no cleanup required).
     */
    override fun disposeUIResources() {
    }
}


/**
 * UI panel class that contains all the configuration options for the code screenshoter plugin.
 * This class creates and manages the Swing UI components for the settings dialog.
 *
 * @property project The current project context for the configuration panel
 */
class CopyImageOptionsPanel(private val project: Project) {
    lateinit var wholePanel: JPanel
    private lateinit var scaleTextField: JTextField
    private lateinit var scaleSlider: JSlider
    private lateinit var chopIndentation: JCheckBox
    private lateinit var removeCaret: JCheckBox
    private lateinit var paddingTextField: JTextField
    private lateinit var saveDirectory: TextFieldWithHistoryWithBrowseButton
    private lateinit var formatComboBox: JComboBox<Format>
    private lateinit var sizeLimitToWarnTextField: JTextField
    private lateinit var dateTimePatternTextField: JTextField

    companion object {
        private const val SLIDER_SCALE = 2.0

    }

    init {
        createUI()
    }

    /**
     * Creates and arranges all UI components for the settings panel using GridBagLayout.
     * This method builds the entire configuration UI with all input fields and controls.
     */
    private fun createUI() {
        wholePanel = JPanel(GridBagLayout())
        wholePanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val constraints = GridBagConstraints()
        constraints.insets = JBUI.insets(2)
        constraints.weightx = 0.0
        constraints.weighty = 0.0
        constraints.fill = GridBagConstraints.HORIZONTAL

        // Scale section
        val scaleLabel = JLabel(CodeScreenshoterBundle.message("options.scale.label"))
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(scaleLabel, constraints)

        scaleTextField = JTextField()
        scaleTextField.isEditable = false
        scaleTextField.horizontalAlignment = SwingConstants.LEFT
        scaleTextField.preferredSize = Dimension(50, scaleTextField.preferredSize.height)
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(scaleTextField, constraints)

        constraints.gridx = 0
        constraints.gridy = 1
        constraints.gridwidth = 2
        constraints.fill = GridBagConstraints.HORIZONTAL
        scaleSlider = JSlider(2, 20)
        scaleSlider.majorTickSpacing = 2
        scaleSlider.minorTickSpacing = 1
        scaleSlider.paintTicks = true
        scaleSlider.snapToTicks = true
        scaleSlider.value = 8
        scaleTextField.text = (scaleSlider.value / SLIDER_SCALE).toString()
        scaleSlider.addChangeListener { _: ChangeEvent ->
            scaleTextField.text = (scaleSlider.value / SLIDER_SCALE).toString()
        }
        wholePanel.add(scaleSlider, constraints)

        // Chop indentation checkbox
        constraints.gridy = 2
        constraints.gridwidth = 1
        constraints.fill = GridBagConstraints.NONE
        chopIndentation = JCheckBox(CodeScreenshoterBundle.message("options.chop.indentation.label"))
        chopIndentation.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        wholePanel.add(chopIndentation, constraints)

        // Remove caret checkbox
        constraints.gridy = 3
        removeCaret = JCheckBox(CodeScreenshoterBundle.message("options.hide.cursor.label"))
        removeCaret.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        wholePanel.add(removeCaret, constraints)

        // Padding section
        val paddingLabel = JLabel(CodeScreenshoterBundle.message("options.padding.label"))
        constraints.gridy = 4
        constraints.gridx = 0
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(paddingLabel, constraints)

        paddingTextField = JTextField()
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        paddingTextField.preferredSize = Dimension(50, paddingTextField.preferredSize.height)
        wholePanel.add(paddingTextField, constraints)

        // Save directory section
        val dirLabel = JLabel(CodeScreenshoterBundle.message("options.save.directory.label"))
        constraints.gridx = 0
        constraints.gridy = 5
        constraints.anchor = GridBagConstraints.LINE_START
        constraints.fill = GridBagConstraints.NONE
        wholePanel.add(dirLabel, constraints)

        val singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        saveDirectory = SwingHelper.createTextFieldWithHistoryWithBrowseButton(
            project,
            singleFolderDescriptor,
            NotNullProducer { ContainerUtil.emptyList() }
        )
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        wholePanel.add(saveDirectory, constraints)

        // Format section
        val formatLabel = JLabel(CodeScreenshoterBundle.message("options.output.format.label"))
        constraints.gridx = 0
        constraints.gridy = 6
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(formatLabel, constraints)

        formatComboBox = ComboBox()
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        constraints.fill = GridBagConstraints.HORIZONTAL
        wholePanel.add(formatComboBox, constraints)

        // Size limit to warn section
        val sizeLimitToWarnLabel = JLabel(CodeScreenshoterBundle.message("options.size.limit.warn.label"))
        constraints.gridx = 0
        constraints.gridy = 7
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(sizeLimitToWarnLabel, constraints)

        sizeLimitToWarnTextField = JTextField()
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        constraints.fill = GridBagConstraints.HORIZONTAL
        wholePanel.add(sizeLimitToWarnTextField, constraints)

        // Date time pattern section
        val dateTimePatternLabel = JLabel(CodeScreenshoterBundle.message("options.datetime.pattern.label"))
        constraints.gridx = 0
        constraints.gridy = 8
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        constraints.anchor = GridBagConstraints.LINE_START
        wholePanel.add(dateTimePatternLabel, constraints)

        dateTimePatternTextField = JTextField()
        constraints.gridx = 1
        constraints.anchor = GridBagConstraints.LINE_START
        constraints.fill = GridBagConstraints.HORIZONTAL
        wholePanel.add(dateTimePatternTextField, constraints)

        // Vertical spacer
        val vSpacer = Box.createVerticalGlue()
        constraints.gridx = 0
        constraints.gridy = 9
        constraints.gridwidth = 2
        constraints.weighty = 1.0
        constraints.fill = GridBagConstraints.VERTICAL
        wholePanel.add(vSpacer, constraints)
    }

    /**
     * Converts the current UI component values to a State object.
     * This method extracts values from all UI controls and creates a State object
     * that represents the current configuration.
     *
     * @return A CopyImageOptionsProvider.State object with values from the UI components
     */
    fun toState(): CopyImageOptionsProvider.State {
        return CopyImageOptionsProvider.State(
            scale = scaleTextField.text.trim().toDoubleOrNull() ?: 4.0,
            padding = paddingTextField.text.trim().toIntOrNull() ?: 0,
            chopIndentation = chopIndentation.isSelected,
            removeCaret = removeCaret.isSelected,
            directoryToSave = saveDirectory.text,
            format = formatComboBox.selectedItem?.let {
                it as Format?
            } ?: Format.PNG,
            sizeLimitToWarn = sizeLimitToWarnTextField.text.trim().toLongOrNull() ?: 3000000L,
            dateTimePattern = dateTimePatternTextField.text.trim()
        )
    }

    /**
     * Sets the UI component values from a State object.
     * This method populates all UI controls with values from the provided State object.
     *
     * @param state The CopyImageOptionsProvider.State object containing values to set in the UI
     */
    fun fromState(state: CopyImageOptionsProvider.State) {
        chopIndentation.isSelected = state.chopIndentation
        removeCaret.isSelected = state.removeCaret
        scaleSlider.value = (state.scale * SLIDER_SCALE).toInt()
        saveDirectory.text = StringUtil.notNullize(state.directoryToSave)
        paddingTextField.text = state.padding.toString()
        formatComboBox.selectedIndex = state.format.ordinal
        sizeLimitToWarnTextField.text = state.sizeLimitToWarn.toString()
        dateTimePatternTextField.text = state.dateTimePattern
    }

    /**
     * Initializes the format selection combobox with available format options.
     * This method should be called after UI creation to populate the format selection dropdown.
     */
    fun init() {
        Format.entries.forEach { item ->
            formatComboBox.addItem(item)
        }
    }
}
