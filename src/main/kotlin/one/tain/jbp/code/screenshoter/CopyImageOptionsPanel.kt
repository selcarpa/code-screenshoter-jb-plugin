package one.tain.jbp.code.screenshoter

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.util.NotNullProducer
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.SwingHelper
import java.awt.*
import javax.swing.*
import javax.swing.event.ChangeEvent

private const val SLIDER_SCALE = 2.0

class CopyImageOptionsPanel(private val project: Project) {
    lateinit var wholePanel: JPanel
    private lateinit var scaleTextField: JTextField
    private lateinit var scaleSlider: JSlider
    private lateinit var chopIndentation: JCheckBox
    private lateinit var removeCaret: JCheckBox
    private lateinit var directorySelectionPanel: JPanel
    private lateinit var paddingTextField: JTextField
    private lateinit var saveDirectory: TextFieldWithHistoryWithBrowseButton
    private lateinit var formatComboBox: JComboBox<Format>
    private lateinit var sizeLimitToWarnTextField: JTextField
    private lateinit var dateTimePatternTextField: JTextField

    init {
        createUI()
    }

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
            CodeScreenshoterBundle.message("configurable.save.directory"),
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

        formatComboBox = JComboBox<Format>()
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

    fun init() {
        Format.entries.forEach { item ->
            formatComboBox.addItem(item)
        }
    }
}
