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

// Scale factor for converting slider values to actual scale values
private const val SLIDER_SCALE = 2.0

/**
 * Helper functions and abstractions for creating UI components in the configuration panel.
 */
private object ConfigurableUIHelper {
    /**
     * Creates a standard label for configuration options.
     */
    fun createLabel(key: String): JLabel {
        return JLabel(CodeScreenshoterBundle.message(key))
    }

    /**
     * Creates a standard text field with specified width.
     */
    fun createTextField(width: Int = 50): JTextField {
        return JTextField().apply {
            preferredSize = Dimension(width, preferredSize.height)
        }
    }

    /**
     * Creates a standard checkbox with bottom and top padding.
     */
    fun createCheckBox(key: String): JCheckBox {
        return JCheckBox(CodeScreenshoterBundle.message(key)).apply {
            border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
        }
    }

    /**
     * Creates a standard GridBagConstraints with default configuration.
     */
    fun createDefaultConstraints(): GridBagConstraints {
        return GridBagConstraints().apply {
            insets = JBUI.insets(2)
            weightx = 0.0
            weighty = 0.0
            fill = GridBagConstraints.HORIZONTAL
        }
    }

    /**
     * Adds a component to the panel with specified row and column.
     */
    fun addToPanel(
        panel: JPanel,
        component: JComponent,
        constraints: GridBagConstraints,
        row: Int,
        col: Int,
        fill: Int = GridBagConstraints.NONE,
        gridWidth: Int = 1,
        anchor: Int = GridBagConstraints.LINE_START,
        weightX: Double = 0.0
    ) {
        constraints.gridx = col
        constraints.gridy = row
        constraints.gridwidth = gridWidth
        constraints.fill = fill
        constraints.anchor = anchor
        constraints.weightx = weightX
        panel.add(component, constraints)
    }

    /**
     * Creates a labeled text field pair in the specified row.
     */
    fun addLabeledTextField(
        panel: JPanel,
        constraints: GridBagConstraints,
        labelKey: String,
        textField: JTextField,
        row: Int
    ): Int {
        val label = createLabel(labelKey)
        addToPanel(panel, label, constraints, row, 0, GridBagConstraints.NONE)
        addToPanel(panel, textField, constraints, row, 1, GridBagConstraints.HORIZONTAL)
        return row + 1
    }
}

/**
 * Configurable class that provides the UI for configuring code screenshoter plugin options.
 * This class implements IntelliJ's SearchableConfigurable interface to integrate the
 * plugin settings into the IDE's settings dialog.
 *
 * @property project The current project for which to manage options
 */
class CopyImageConfigurable(private val project: Project) : SearchableConfigurable, NoScroll {
    private val panel: CopyImageOptionsPanel by lazy {
        CopyImageOptionsPanel(project).apply {
            init()
        }
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
    private lateinit var lineLimitToWarnTextField: JTextField
    private lateinit var dateTimePatternTextField: JTextField
    private lateinit var gutterIconsShown: JCheckBox
    private lateinit var foldingOutlineShown: JCheckBox
    private lateinit var innerWhitespaceShown: JCheckBox
    private lateinit var indentGuidesShown: JCheckBox
    private lateinit var lineNumbersShown: JCheckBox

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

        val constraints = ConfigurableUIHelper.createDefaultConstraints()

        var currentRow = 0

        // Scale section - slider and text field
        currentRow = createScaleSection(constraints, currentRow)

        // Basic options section - checkboxes
        currentRow = createBasicOptionsSection(constraints, currentRow)

        // Input fields section - padding, directory, format, etc.
        currentRow = createInputFieldsSection(constraints, currentRow)

        // Gutter options section - checkboxes for gutter display
        currentRow = createGutterOptionsSection(constraints, currentRow)

        // Vertical spacer to take up remaining space
        createVerticalSpacer(constraints, currentRow)
    }

    /**
     * Creates the scale section with slider and text field.
     */
    private fun createScaleSection(constraints: GridBagConstraints, row: Int): Int {
        val currentRow = row

        // Scale label and text field
        val scaleLabel = ConfigurableUIHelper.createLabel("options.scale.label")
        ConfigurableUIHelper.addToPanel(wholePanel, scaleLabel, constraints, currentRow, 0, GridBagConstraints.NONE)

        scaleTextField = JTextField().apply {
            isEditable = false
            horizontalAlignment = SwingConstants.LEFT
            preferredSize = Dimension(50, preferredSize.height)
        }
        ConfigurableUIHelper.addToPanel(wholePanel, scaleTextField, constraints, currentRow, 1, GridBagConstraints.NONE, anchor = GridBagConstraints.LINE_START)

        // Scale slider (spans both columns)
        constraints.gridx = 0
        constraints.gridy = currentRow + 1
        constraints.gridwidth = 2
        constraints.fill = GridBagConstraints.HORIZONTAL

        scaleSlider = JSlider(2, 20).apply {
            majorTickSpacing = 2
            minorTickSpacing = 1
            paintTicks = true
            snapToTicks = true
            value = 8
            // Set initial text value based on slider
            scaleTextField.text = (value / SLIDER_SCALE).toString()
            addChangeListener { _: ChangeEvent ->
                scaleTextField.text = (value / SLIDER_SCALE).toString()
            }
        }
        wholePanel.add(scaleSlider, constraints)

        return currentRow + 2
    }

    /**
     * Creates the basic options section with checkboxes.
     */
    private fun createBasicOptionsSection(constraints: GridBagConstraints, row: Int): Int {
        var currentRow = row

        // Chop indentation checkbox
        chopIndentation = ConfigurableUIHelper.createCheckBox("options.chop.indentation.label")
        ConfigurableUIHelper.addToPanel(wholePanel, chopIndentation, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        // Remove caret checkbox
        removeCaret = ConfigurableUIHelper.createCheckBox("options.hide.cursor.label")
        ConfigurableUIHelper.addToPanel(wholePanel, removeCaret, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        return currentRow
    }

    /**
     * Creates the input fields section with padding, directory, format, etc.
     */
    private fun createInputFieldsSection(constraints: GridBagConstraints, row: Int): Int {
        var currentRow = row

        // Padding text field with label
        paddingTextField = ConfigurableUIHelper.createTextField()
        currentRow = ConfigurableUIHelper.addLabeledTextField(wholePanel, constraints, "options.padding.label", paddingTextField, currentRow)

        // Save directory section
        val dirLabel = ConfigurableUIHelper.createLabel("options.save.directory.label")
        ConfigurableUIHelper.addToPanel(wholePanel, dirLabel, constraints, currentRow, 0, GridBagConstraints.NONE)

        val singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        saveDirectory = SwingHelper.createTextFieldWithHistoryWithBrowseButton(
            project,
            singleFolderDescriptor,
            NotNullProducer { ContainerUtil.emptyList() }
        )
        ConfigurableUIHelper.addToPanel(wholePanel, saveDirectory, constraints, currentRow, 1, GridBagConstraints.HORIZONTAL, weightX = 1.0)
        currentRow++

        // Format section
        val formatLabel = ConfigurableUIHelper.createLabel("options.output.format.label")
        ConfigurableUIHelper.addToPanel(wholePanel, formatLabel, constraints, currentRow, 0, GridBagConstraints.NONE)

        formatComboBox = ComboBox<Format>()
        ConfigurableUIHelper.addToPanel(wholePanel, formatComboBox, constraints, currentRow, 1, GridBagConstraints.HORIZONTAL)
        currentRow++

        // Date time pattern section
        dateTimePatternTextField = ConfigurableUIHelper.createTextField()
        currentRow = ConfigurableUIHelper.addLabeledTextField(wholePanel, constraints, "options.datetime.pattern.label", dateTimePatternTextField, currentRow)

        // Line limit to warn section
        lineLimitToWarnTextField = ConfigurableUIHelper.createTextField()
        currentRow = ConfigurableUIHelper.addLabeledTextField(wholePanel, constraints, "options.line.limit.warn.label", lineLimitToWarnTextField, currentRow)

        return currentRow
    }

    /**
     * Creates the gutter options section with checkboxes for gutter display settings.
     */
    private fun createGutterOptionsSection(constraints: GridBagConstraints, row: Int): Int {
        var currentRow = row

        // Gutter icons shown checkbox
        gutterIconsShown = ConfigurableUIHelper.createCheckBox("options.gutter.icons.shown.label")
        ConfigurableUIHelper.addToPanel(wholePanel, gutterIconsShown, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        // Folding outline shown checkbox
        foldingOutlineShown = ConfigurableUIHelper.createCheckBox("options.folding.outline.shown.label")
        ConfigurableUIHelper.addToPanel(wholePanel, foldingOutlineShown, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        // Inner whitespace shown checkbox
        innerWhitespaceShown = ConfigurableUIHelper.createCheckBox("options.inner.whitespace.shown.label")
        ConfigurableUIHelper.addToPanel(wholePanel, innerWhitespaceShown, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        // Indent guides shown checkbox
        indentGuidesShown = ConfigurableUIHelper.createCheckBox("options.indent.guides.shown.label")
        ConfigurableUIHelper.addToPanel(wholePanel, indentGuidesShown, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        // Line numbers shown checkbox
        lineNumbersShown = ConfigurableUIHelper.createCheckBox("options.line.numbers.shown.label")
        ConfigurableUIHelper.addToPanel(wholePanel, lineNumbersShown, constraints, currentRow, 0, GridBagConstraints.NONE)
        currentRow++

        return currentRow
    }

    /**
     * Creates a vertical spacer to fill remaining space in the panel.
     */
    private fun createVerticalSpacer(constraints: GridBagConstraints, row: Int) {
        val vSpacer = Box.createVerticalGlue()
        constraints.gridx = 0
        constraints.gridy = row
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
            lineLimitToWarn = lineLimitToWarnTextField.text.trim().toLongOrNull() ?: 50L,
            dateTimePattern = dateTimePatternTextField.text.trim(),
            gutterIconsShown = gutterIconsShown.isSelected,
            foldingOutlineShown = foldingOutlineShown.isSelected,
            innerWhitespaceShown = innerWhitespaceShown.isSelected,
            indentGuidesShown = indentGuidesShown.isSelected,
            lineNumbersShown = lineNumbersShown.isSelected
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
        lineLimitToWarnTextField.text = state.lineLimitToWarn.toString()
        dateTimePatternTextField.text = state.dateTimePattern
        gutterIconsShown.isSelected = state.gutterIconsShown
        foldingOutlineShown.isSelected = state.foldingOutlineShown
        innerWhitespaceShown.isSelected = state.innerWhitespaceShown
        indentGuidesShown.isSelected = state.indentGuidesShown
        lineNumbersShown.isSelected = state.lineNumbersShown
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
