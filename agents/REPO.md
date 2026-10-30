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

## Maintenance mode

This repo is in maintenance mode (owner decision, 2026-10-30): **it keeps up with
the template but gets no new features of its own** until the mode lifts.

- Included in every template wave - rulebook pulls, dependency pins, CI and
  tooling updates, and new template content land here as they do everywhere.
- No repo-initiated feature work while the mode lasts.

Decision: `HexaHexes/neoforge-mod-template#52` (maintenance-mode note);
tracking: `HexaHexes/neoforge-mod-template#60`.
## Notes

- This repo now ships `release.yml`, `publish.yml` and `version-bump.yml`. The
  template's `ci.yml` is deliberately not copied: this repo has no JUnit or
  GameTest suite, so its three-check CI would fail every run - bring it when a
  suite lands.
