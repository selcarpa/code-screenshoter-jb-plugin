package one.tain.jbp.code.screenshoter

import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import javax.swing.*

class CopyImageConfigurable(private val myProject: Project) : SearchableConfigurable, NoScroll {
    private lateinit var myPanel: CopyImageOptionsPanel

    override fun getDisplayName(): @Nls String {
        return CodeScreenshoterBundle.message("configurable.display.name")
    }

    override fun getId(): String {
        return "one/tain/jbp/code/screenshoter"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent {
        myPanel = CopyImageOptionsPanel(myProject)
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
}
