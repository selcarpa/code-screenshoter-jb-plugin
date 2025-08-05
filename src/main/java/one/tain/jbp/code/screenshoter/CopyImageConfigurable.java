package one.tain.jbp.code.screenshoter;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.SwingHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author Tagir Valeev
 */
public class CopyImageConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private CopyImageOptionsPanel myPanel;
    private final Project myProject;

    public CopyImageConfigurable(Project project) {
        this.myProject = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Copy code as image";
    }

    @NotNull
    @Override
    public String getId() {
        return "screenshoter";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        myPanel = new CopyImageOptionsPanel();
        myPanel.init();
        return myPanel.wholePanel;
    }

    @Override
    public boolean isModified() {
        CopyImageOptionsProvider provider = myProject.getService(CopyImageOptionsProvider.class);
        return !provider.getState().equals(myPanel.toState());
    }

    @Override
    public void apply() {
        CopyImageOptionsProvider provider = myProject.getService(CopyImageOptionsProvider.class);
        provider.loadState(myPanel.toState());
    }

    @Override
    public void reset() {
        CopyImageOptionsProvider provider = myProject.getService(CopyImageOptionsProvider.class);
        myPanel.fromState(provider.getState());
    }

    @Override
    public void disposeUIResources() {
        myPanel = null;
    }

    public class CopyImageOptionsPanel {
        private static final double SLIDER_SCALE = 2.0;

        private JTextField scale;
        private JCheckBox chopIndentation;
        private JCheckBox removeCaret;
        private JPanel wholePanel;
        private JSlider slider;
        private JPanel saveDirectoryPanel;
        private JTextField padding;
        private TextFieldWithHistoryWithBrowseButton saveDirectory;
        private JComboBox<TransferableImage.Format> format;

        CopyImageOptionsProvider.State toState() {
            CopyImageOptionsProvider.State state = new CopyImageOptionsProvider.State();
            state.myChopIndentation = chopIndentation.isSelected();
            state.myRemoveCaret = removeCaret.isSelected();
            try {
                state.myScale = Double.parseDouble(scale.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            state.myDirectoryToSave = StringUtil.nullize(saveDirectory.getText());
            try {
                state.myPadding = Integer.parseInt(padding.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            state.myFormat = (TransferableImage.Format) format.getSelectedItem();

            return state;
        }

        void fromState(CopyImageOptionsProvider.State state) {
            chopIndentation.setSelected(state.myChopIndentation);
            removeCaret.setSelected(state.myRemoveCaret);
            slider.setValue((int) (state.myScale * SLIDER_SCALE));
            saveDirectory.setText(StringUtil.notNullize(state.myDirectoryToSave));
            padding.setText(String.valueOf(state.myPadding));
            format.setSelectedIndex(state.myFormat == null ? 0 : state.myFormat.ordinal());
        }

        void init() {
            slider.addChangeListener(e -> scale.setText(String.valueOf(slider.getValue() / SLIDER_SCALE)));
            Arrays.stream(TransferableImage.Format.values()).forEach(format::addItem);
        }

        private void createUIComponents() {
            FileChooserDescriptor singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            TextFieldWithHistoryWithBrowseButton field = SwingHelper.createTextFieldWithHistoryWithBrowseButton(
                    myProject,
                    "Save to Directory",
                    singleFolderDescriptor,
                    ContainerUtil::emptyList);
            saveDirectoryPanel = field;
            saveDirectory = field;
        }
    }
}
