# Royal Crown - MineColonies

[CurseForge](https://www.curseforge.com/members/thecyber27/projects) |
[GitHub](https://github.com/HyanFerreira) |
[Issues](ISSUES_LINK_HERE)

---

## About

**You built the colony. Now let it recognize you as its ruler.**

Royal Crown - MineColonies is a lightweight narrative expansion for
[MineColonies](https://www.curseforge.com/minecraft/mc-mods/minecolonies).

In MineColonies, you lead, build, feed, defend, and expand a settlement, but the world does not always treat you like
its ruler. Guards may retaliate after an accidental hit, and your authority over the colony's defense can feel limited.

Royal Crown changes that fantasy. You do not simply craft a crown and call yourself king. You earn legitimacy, prove the
colony can survive, stand before your people, and receive the King's Crown through a witnessed coronation.

---

## Features

### Royal Advisor

A Royal Advisor arrives near your Town Hall and begins your path toward the crown.

- Meet the Advisor near the MineColonies Town Hall.
- Accept the royal trials.
- Track your progress through dialogue or commands.
- Return when your colony is ready to witness your coronation.

---

### Royal Trials

The crown is not a free cosmetic item. It is earned.

Current trial requirements include:

- reaching a minimum colony population;
- completing defenses near the colony;
- proving that you can protect the people you intend to rule.

Only after completing the trials can you claim the crown.

---

### Coronation Ceremony

Becoming king is no longer just a button press.

When the trials are complete, the Royal Advisor gathers nearby citizens and guards around the Town Hall. Citizens walk
toward the ceremony area, look toward their future king, cheer, jump, and celebrate as the crown is granted.

The ceremony includes:

- Town Hall-centered gathering;
- citizens and guards called through pathfinding;
- randomized citizen cheering;
- particles, sounds, and golden fireworks;
- final crown grant only after requirements are revalidated.

The ceremony can be tuned or disabled through config.

---

### King's Crown

The King's Crown is the symbol of recognized authority.

While wearing it:

- MineColonies guards and citizens do not retaliate against accidental hits from the crowned player.
- Nearby guards can assist when the king attacks a valid target.
- Nearby guards can defend the king when attacked.
- Protected targets such as colonists, guards, tamed animals, and players are kept out of forced attack behavior by
  default.

The goal is not to make the player overpowered. The goal is to make the colony feel like it knows who its ruler is.

---

### Commands

Royal Crown includes commands for normal play and local testing.

Useful commands:

- `/royalcrown help`
- `/royalcrown status`
- `/royalcrown advisor where`

Debug/testing commands:

- `/royalcrown debug accept`
- `/royalcrown debug complete_defenses`
- `/royalcrown debug start_coronation`
- `/royalcrown debug reset_player`
- `/royalcrown debug clear_crown`
- `/royalcrown debug give_crown`
- `/royalcrown debug respawn_advisor`

Debug commands are currently open and intended to make development/testing easier.

---

## Configuration

Royal Crown generates `royalcrown-common.toml`.

You can customize:

- required citizens;
- required defenses;
- defense wave size and timeout;
- guard assistance radius;
- crown behavior against guard retaliation;
- forced guard attack damage/cooldown tuning;
- unique crown ownership per world;
- coronation duration;
- coronation participant search radius;
- maximum ceremony participants;
- citizen jump chance during celebration;
- whether the coronation ceremony is enabled.

---

## Installation

Required:

- Minecraft 1.20.1
- Forge 47+
- Java 17+
- MineColonies

Recommended:

- Install Royal Crown on both client and server.
- Use it in worlds where MineColonies is part of the main progression.

---

## Notes

- The mod is designed around MineColonies and is best experienced with an active colony.
- The Crown is intended to be earned through the Royal Advisor, not crafted freely.
- Guard behavior is intentionally scoped around recognition and protection, not full RTS control.
- Coronation behavior uses pathfinding and loaded nearby citizens; unloaded or unreachable citizens may not attend.

---

## Credits

Created by **Hyan Ferreira**.

Built as a royal progression expansion for **MineColonies**.
