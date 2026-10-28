package io.github.tser239.materialicons.icons

/**
 * A single user-defined association, also used as the persisted state type.
 *
 * Properties are mutable and the class has a no-arg constructor (all parameters have
 * defaults) so that [com.intellij.openapi.components.PersistentStateComponent]'s
 * `XmlSerializer` can round-trip it using JavaBean conventions.
 */
data class IconAssociation(
    var kind: AssociationKind = AssociationKind.EXTENSION,
    var pattern: String = "",
    var iconId: String = "",
)
