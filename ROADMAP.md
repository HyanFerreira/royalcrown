# Royal Crown Roadmap

Royal Crown is a MineColonies addon built around one core fantasy: the player is already the founder/governor of a colony, but MineColonies does not always treat them as such. This mod exists to make the colony recognize the player as its ruler.

The crown should not feel like a free craftable item or a raw power buff. It should feel like a title earned through legitimacy, defense, growth, and recognition from the colony.

## Current Identity

Royal Crown currently adds:

- A Royal Advisor NPC that appears near the MineColonies Town Hall.
- A dialogue flow where the player accepts royal trials.
- Trial progress based on minimum colony population and completed defenses.
- A King's Crown item awarded by the Advisor after completing the trials.
- Crown behavior that prevents MineColonies guards/citizens from retaliating against the crowned player.
- Crown behavior that lets nearby guards assist the crowned player against attacked or attacking entities.
- Commands for checking progress and locating the Advisor.
- Advancements for the royal progression.

## Current Version Target

- Next public release target: `1.1.0`.
- Rationale: the coronation ceremony and near-term royal command features are meaningful additions, but they preserve the current mod identity and progression model.
- Reserve `2.0.0` for a larger redesign, such as a full legitimacy system, royal raid progression, crown tiers, or major save/progression changes.

## Local Build Notes

- This machine has a working Java 17 installation at `/home/hyanferreira/.jdks/ms-17.0.19`.
- Build command used locally:

```sh
JAVA_HOME=/home/hyanferreira/.jdks/ms-17.0.19 PATH=/home/hyanferreira/.jdks/ms-17.0.19/bin:$PATH ./gradlew build
```

- `gradlew` should be executable in this repo.
- IntelliJ/VS Code may warn about broken JDK 24 toolchain entries under `~/.jdks`; those warnings did not block the build when Java 17 was selected.

## Design Pillars

### Recognition

The colony should acknowledge the player as ruler.

Examples:

- Guards should not retaliate against accidental hits from the crowned player.
- Advisor dialogue should treat the player as a future or current monarch.
- Coronation and post-coronation events should make the colony feel aware of the player's title.

### Command

The crown should represent authority over the colony's defense without becoming uncontrolled automation.

Examples:

- Guards may attack the king's target.
- Guards may defend the king when attacked.
- Future royal decrees may allow simple temporary orders such as defend the king, protect the Town Hall, attack the king's target, or cease attack.

### Legitimacy

The crown should be earned, not crafted freely.

Examples:

- The player must develop and defend the colony.
- The Royal Advisor exists to give narrative weight to the title.
- Future trials should involve colony growth, defense infrastructure, and a final witnessed coronation.

## Important Context

- The `crown.json` recipe currently points to `royalcrown:crown_` on purpose, so the crown is not normally craftable. This was an intentional design choice to force the player to earn the crown through the Advisor.
- If this causes JEI/datapack/log confusion later, replace it with a cleaner non-craftable approach instead of making the real crown craftable.
- MineColonies integration currently uses reflection in several places to avoid depending too tightly on a specific MineColonies API shape.
- Citizen counting currently has multiple paths. Some flows use exact/reflection-based colony count, while others use nearby entity count. This can create inconsistent progress readings and should be unified.
- Advisor spawn state currently exists in more than one data class. This should be consolidated before the progression system grows.

## Near-Term Roadmap

### 1. Polish Current Progression

Goal: make the existing crown journey more reliable before adding bigger features.

- [x] Unify citizen counting so Advisor dialogue, crown claim validation, and `/royalcrown status` all use the same logic.
- [x] Improve `/royalcrown status` with accepted/crowned/ceremony/crown-owner state.
- [x] Add open debug commands for development and local testing:
  - accept trials;
  - complete defense requirement for current player;
  - reset current player progress;
  - respawn/ensure Advisor near the Town Hall;
  - clear world crown owner;
  - give crown to current player.
- Review duplicate or obsolete saved data classes for Advisor spawn state.
- Decide whether to remove the invalid crown recipe or replace it with a cleaner intentional lock.

### 2. Coronation Ceremony

Goal: make the moment of becoming king feel witnessed by the colony.

Proposed flow:

1. [x] Player completes the trials and speaks with the Royal Advisor.
2. [x] Ceremony starts near the Town Hall.
3. [x] Nearby citizens and guards are called toward the ceremony area using pathfinding, not teleportation.
4. [x] Citizens and guards look toward the player.
5. [x] During the celebration phase, citizens randomly jump to look happy.
6. [x] Guards use a closer ceremony radius than citizens.
7. [x] Sounds, particles, golden fireworks, and royal messages play.
8. [x] The crown is granted at the end of the ceremony after requirements are revalidated.
9. [ ] Add richer Advisor dialogue before the ceremony starts.
10. [ ] Improve formation quality if MineColonies pathfinding allows it.
11. [ ] Optionally trigger the farewell/post-coronation dialogue automatically after the ceremony.

Implementation notes:

- Locate the nearest Town Hall and use it as the ceremony anchor.
- Search for `minecolonies:citizen` entities around the Town Hall.
- Treat citizens with weapons as likely guards when formation logic is needed.
- Avoid teleporting citizens unless absolutely necessary.
- Use short-lived AI nudges: navigation target, look control, occasional jump.
- Use randomized jump timing so citizens do not all jump in the same tick.
- Ceremony should fail gracefully if few citizens can reach the area.

### 3. Royal Decrees

Goal: formalize the current "guards obey the king" behavior into readable player-facing commands.

Potential decrees:

- Attack My Target: nearby guards focus the entity the king attacked.
- Defend the King: nearby guards prioritize entities attacking the king for a limited duration.
- Protect the Town Hall: guards prioritize hostiles near the Town Hall.
- Cease Arms: clears the forced target/order.

Design constraints:

- Decrees should have cooldowns or duration limits.
- They should not make guards attack protected colonists, tamed animals, or other players by default.
- They should be understandable through chat/actionbar feedback.

### 4. Expanded Royal Trials

Goal: make earning the crown feel more connected to colony development.

Possible trial categories:

- Foundation: Town Hall reaches a configured level.
- Prosperity: colony reaches population threshold.
- Defense: guard tower, barracks, or completed defenses.
- Loyalty: avoid citizen deaths during a trial window.
- Final Trial: a special defense or Royal Raid before coronation.

### 5. Royal Advisor After Coronation

Goal: keep the Advisor useful after the crown is claimed.

Ideas:

- Advisor returns periodically with letters or missions.
- Advisor provides status on colony legitimacy and future goals.
- Advisor can start optional royal events.
- Advisor can explain or configure royal decrees.

## Larger Ideas

- Royal Raid: a special final attack event that proves the colony can defend itself before coronation.
- Legitimacy score: reputation-like progress based on growth, defense, safety, and prosperity.
- Crown tiers: simple crown, royal crown, imperial crown, unlocked by colony milestones.
- Decorative royal blocks/items: banner, seal, throne, proclamation table.
- Curios compatibility: allow wearing the crown without sacrificing the helmet slot.
- Datapack-driven trial requirements for modpack authors.
- Better compatibility with MineColonies raids/invasions if the API supports it.

## Changelog / Development Log

### 2026-06-29

- Created this roadmap to preserve project context across machines and future conversations.
- Captured the current design intent: Royal Crown is about recognition, command, and legitimacy inside MineColonies.
- Added the Coronation Ceremony as the next major feature candidate.
- Documented that the invalid `crown_` recipe target is intentional for now, used to keep the crown from being craftable.
- Implemented the first Coronation Ceremony pass:
  - claiming the crown now starts a short server-side ceremony instead of granting the crown immediately;
  - nearby MineColonies citizens are called toward the Town Hall area through pathfinding;
  - citizens and guards look at the player during the ceremony;
  - citizens randomly jump and emit happy particles during the celebration phase;
  - the crown is granted only at the end, after the trials are revalidated.
- Centralized crown claim validation and final crown granting in `RoyalTrials`.
- Updated `/royalcrown status` to use the same colony citizen count as the Advisor flow and show accepted/crowned/ceremony/crown-owner state.
- Configured local builds to use the existing Microsoft JDK 17 at `/home/hyanferreira/.jdks/ms-17.0.19`.
- Made `gradlew` executable.
- Bumped `mod_version` to `1.1.0` for the next public release target.
- Verified `./gradlew build` successfully with Java 17.
- Added open `/royalcrown debug ...` commands for faster local testing without permission requirements.
