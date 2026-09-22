# StarboundMC UI Design Skill

Repository-scoped Codex skill for UI/art direction and UI critique.

Recommended install path:

`.agents/skills/starboundmc-ui-design/`

This skill is intentionally separate from `ldlib2-ui`:
- `starboundmc-ui-design` decides the interaction concept, visual hierarchy, and Design Contract.
- `ldlib2-ui` implements an approved design using LDLib2.

The skill permits implicit invocation, but its `description` is intentionally scoped to design/redesign/review work so that small implementation-only fixes should not trigger it.

If Codex does not notice the newly added skill, restart Codex. See the current OpenAI Codex skill documentation for repository skill discovery and invocation behavior.
