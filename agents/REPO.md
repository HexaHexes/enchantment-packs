# Repo rules - Enchantment Packs

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in `neoforge-mod-template` and pulled in verbatim - **never hand-edit it.**
Everything in this file is this repo's own and is edited freely; keep it aligned
with the org rules rather than restating them.

**Repo:** `HexaHexes/enchantment-packs` · **Mod ID:** `enchantment_packs` · **Branch:** `main`
**Jar:** `build/libs/enchantment_packs-<version>.jar`

Registers attribute-focused enchantments from external folder or zip packs.
Enchantments apply from equipped items only; managed attribute modifiers refresh
on equipment change and a short tick interval; negative values are supported for
decreases.

**Test:** no GameTest suite in this repo - verify with `./gradlew build`, and with
`./gradlew runData` after touching generated content.

**Remote:** `github.com/HexaHexes/enchantment-packs` (public, default branch
`main`).

---

## Notes

- This repo predates the org's current workflow conventions: it ships no
  `.github/workflows` at all (no CI checks run on its PRs). Bring it onto the
  org baseline deliberately, not as a drive-by.
