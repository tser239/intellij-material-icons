package io.github.tser239.materialicons.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import io.github.tser239.materialicons.icons.IconAssociation
import javax.swing.JCheckBox

/**
 * Settings page under **Settings | Appearance & Behavior | Material Icons**.
 *
 * The controls are read explicitly in [isModified] and [apply] instead of via `bindSelected`,
 * because the page also has a free-text editor that has no DSL binding: keeping one source of
 * truth (the Swing components) avoids the classic "Apply stays disabled" bug that appears when
 * `isModified()` looks at a property that is only written on apply.
 */
class MaterialIconsConfigurable : BoundConfigurable("Material Icons") {

    private val settings = MaterialIconsSettings.getInstance()

    private var enabledCheckBox: JCheckBox? = null
    private var applyDefaultIconsCheckBox: JCheckBox? = null
    private var associationsArea: JBTextArea? = null

    override fun createPanel(): DialogPanel = panel {
        group("General") {
            row {
                enabledCheckBox = checkBox("Enable Material file and folder icons").component
            }
            row {
                applyDefaultIconsCheckBox = checkBox("Apply Material icons to files and folders without a custom association")
                    .comment("When disabled, only files and folders that match the theme get a Material icon.")
                    .component
            }
        }

        group("Custom associations") {
            row {
                comment(
                    "One per line: <b>kind:pattern=icon-id</b> &mdash; for example " +
                        "<code>extension:kt=kotlin</code>, <code>file:Makefile=make</code> or " +
                        "<code>folder:.github=github</code>. Lines starting with <code>#</code> are ignored."
                )
            }
            row {
                val area = JBTextArea(AssociationTextFormat.format(settings.currentState.associations), 8, 80).apply {
                    lineWrap = true
                    wrapStyleWord = true
                }
                associationsArea = area
                cell(JBScrollPane(area))
                    .align(Align.FILL)
            }.resizableRow()
            row {
                comment("Icon ids are defined by the bundled theme; unknown ids are ignored and logged once.")
            }
        }

        row {
            button("Refresh icons") {
                MaterialIconsRefresher.refresh()
            }.comment("Re-applies icons to all open projects. Only needed if a file does not repaint on its own.")
        }
    }

    override fun isModified(): Boolean {
        val state = settings.currentState
        return uiEnabled != state.enabled ||
            uiApplyDefaultIcons != state.applyDefaultIcons ||
            currentAssociations() != state.associations
    }

    override fun apply() {
        val state = settings.currentState
        val associations = currentAssociations()
        val changed = uiEnabled != state.enabled ||
            uiApplyDefaultIcons != state.applyDefaultIcons ||
            associations != state.associations

        state.enabled = uiEnabled
        state.applyDefaultIcons = uiApplyDefaultIcons
        // Replace instead of mutating: icon providers may be iterating an older snapshot.
        state.associations = associations.toMutableList()

        if (changed) {
            MaterialIconsRefresher.refresh()
        }
    }

    override fun reset() {
        val state = settings.currentState
        enabledCheckBox?.isSelected = state.enabled
        applyDefaultIconsCheckBox?.isSelected = state.applyDefaultIcons
        associationsArea?.text = AssociationTextFormat.format(state.associations)
    }

    private val uiEnabled: Boolean
        get() = enabledCheckBox?.isSelected ?: settings.currentState.enabled

    private val uiApplyDefaultIcons: Boolean
        get() = applyDefaultIconsCheckBox?.isSelected ?: settings.currentState.applyDefaultIcons

    private fun currentAssociations(): List<IconAssociation> =
        AssociationTextFormat.parse(
            associationsArea?.text ?: AssociationTextFormat.format(settings.currentState.associations)
        )
}
