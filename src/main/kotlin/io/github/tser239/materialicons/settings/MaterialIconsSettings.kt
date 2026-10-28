package io.github.tser239.materialicons.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import io.github.tser239.materialicons.icons.IconAssociation

/**
 * Application-level settings. Kept small and allocation-free on the read path: the icon providers
 * only read the current state and look at a few plain fields.
 *
 * Fields are volatile because they are written on the EDT (settings page) and read from whatever
 * thread asks for an icon; the association list is replaced as a whole, never mutated in place.
 */
@State(name = "MaterialIconsSettings", storages = [Storage("material-icons.xml")])
class MaterialIconsSettings : PersistentStateComponent<MaterialIconsSettings.State> {

    @Volatile
    private var myState = State()

    /** Convenience accessor; `state` would clash with [getState] on the JVM. */
    val currentState: State
        get() = myState

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    class State {
        @Volatile
        var enabled: Boolean = true

        @Volatile
        var applyDefaultIcons: Boolean = false

        /**
         * Replaced as a whole by the settings UI instead of being mutated in place, so a provider
         * iterating an old snapshot can never observe a concurrent modification.
         */
        @Volatile
        var associations: MutableList<IconAssociation> = mutableListOf()
    }

    companion object {
        fun getInstance(): MaterialIconsSettings = service()
    }
}
