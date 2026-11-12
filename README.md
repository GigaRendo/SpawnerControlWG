# Spawner Control (Fabric, MC 26.3, server-side)

All mob spawners are disabled by default. Trial spawners are untouched.
Config is saved to `config/spawnercontrol.json`. Commands need op level 2. `/sc` = `/spawnercontrol`.

| Command | Effect |
|---|---|
| `/sc <x y z> <radius> [range]` | Enable spawners within radius (sphere) of the point. Range omitted = default range |
| `/sc mob allow <mob>` / `deny <mob>` / `list` / `clear` | Global whitelist: spawners of that mob work everywhere |
| `/sc default <true/false>` | Enable/disable every spawner by default |
| `/sc defaultrange <blocks>` | Default player range (16) |
| `/sc list` / `info <id>` / `remove <id>` | Manage regions |
| `/sc edit <id> radius\|range\|count\|mindelay\|maxdelay\|maxnearby\|spawnrange <n\|default>` | Region spawn settings |
| `/sc edit <id> always <true/false>` | Works with no player nearby |
| `/sc edit <id> loot <x>` / `xp <x>` | Loot / XP multiplier for mobs from this region's spawners (0 = none) |
| `/sc edit <id> mob add\|remove\|clear <mob>` | Per-region mob filter (empty = any mob) |

Build: push to GitHub, open the Actions tab, download the `spawnercontrol` artifact.
