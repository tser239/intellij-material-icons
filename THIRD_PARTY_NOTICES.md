# Third-party notices

The plugin's own code is licensed under the MIT License; see the repository's `LICENSE` file
(Copyright (c) 2026 tser239). The notices below cover the third-party material it redistributes.

## Material Icon Theme

The SVG icons under `src/main/resources/themes/material/icons/` (1256 upstream drawings) and the
theme manifest `src/main/resources/themes/material/material-icons.json` are from
[Material Icon Theme](https://github.com/material-extensions/vscode-material-icon-theme) version
5.40.0, Copyright (c) 2025 Material Extensions.

Licensed under the MIT License. The full license text is bundled at
`src/main/resources/themes/material/LICENSE`.

The folder icons are derivative works of that artwork: the folder body has been replaced with the
IntelliJ Platform silhouette (see below) and the original artwork has been uniformly scaled and
centred inside it, with an additional `_dark` variant per folder. Everything else — colours,
symbols, the manifest and all file icons — is unchanged.

The upstream project also credits the authors of individual icons; see its repository for the full
list of attributions.

## IntelliJ Platform folder icon

The folder silhouette in every re-skinned folder icon comes from
`expui/nodes/folder.svg` in the IntelliJ Platform
([intellij-community](https://github.com/JetBrains/intellij-community)), Copyright 2000-2022
JetBrains s.r.o. It is licensed under the Apache License 2.0; the full text is bundled with the
plugin at `licenses/APACHE-2.0.txt`.

Changes made to that file, as required by section 4(b) of the license:

- only the `d` attribute of its single `<path>` element is reused; nothing else is copied;
- the path is recoloured per folder using the Material Icon Theme colour, and stroked instead of
  drawn as a second element, to produce the light and dark variants;
- it is embedded into 271 folder SVGs whose artwork is then scaled, translated and wrapped in a
  group, so the resulting files are derivative works rather than copies of the original file.

## Gson

`com.google.code.gson:gson:2.13.2` is bundled in the plugin's `lib/` directory and is licensed
under the Apache License 2.0; the full text is bundled at `licenses/APACHE-2.0.txt`.
