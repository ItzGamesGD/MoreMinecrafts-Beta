### Route sweep, scale 0.5 / speed 0.25 (arrival tick at radius 1.5, ticks after spawn)

| side | start | trials | arrived | min | median | p90 | p95 | max |
|---|---|---|---|---|---|---|---|---|
| E | BOSS_E | 30 | 30 | 155 | 155 | 155 | 155 | 156 |
| W | BOSS_E | 30 | 30 | 155 | 156 | 156 | 156 | 156 |
| E | BOSS_N | 30 | 30 | 216 | 218 | 218 | 218 | 232 |
| W | BOSS_N | 30 | 30 | 216 | 217 | 218 | 218 | 232 |
| E | BOSS_S | 30 | 30 | 146 | 153 | 154 | 155 | 155 |
| W | BOSS_S | 30 | 30 | 146 | 153 | 154 | 155 | 155 |
| E | BOSS_W | 30 | 30 | 206 | 207 | 207 | 207 | 207 |
| W | BOSS_W | 30 | 30 | 205 | 206 | 207 | 207 | 207 |
| E | CORRIDOR_OUT | 30 | 30 | 329 | 329 | 330 | 330 | 330 |
| W | CORRIDOR_OUT | 30 | 30 | 329 | 330 | 330 | 330 | 330 |
| E | DAIS_FRONT | 30 | 30 | 128 | 132 | 154 | 155 | 155 |
| W | DAIS_FRONT | 30 | 30 | 128 | 130.5 | 154 | 154 | 154 |
| E | DOOR_IN | 30 | 30 | 295 | 296 | 296 | 296 | 296 |
| W | DOOR_IN | 30 | 30 | 295 | 295 | 296 | 296 | 296 |
| E | NW_CORNER | 30 | 30 | 349 | 349 | 350 | 350 | 350 |
| W | NW_CORNER | 30 | 30 | 349 | 350 | 350 | 350 | 350 |
| E | OBELISK_N | 30 | 30 | 209 | 209 | 210 | 210 | 210 |
| W | OBELISK_N | 30 | 30 | 209 | 210 | 210 | 210 | 210 |

### East vs West (mirror) comparison, median arrival tick and mirrored final x offset from the stop centre

| start | E median | W median | W-E | E spread | W spread | E final dx | W final dx (mirrored) |
|---|---|---|---|---|---|---|---|
| BOSS_E | 155 | 156 | 1 | 1 | 1 | 0.87 | 0.87 |
| BOSS_N | 218 | 217 | -1 | 16 | 16 | -1.21 | -1.22 |
| BOSS_S | 153 | 153 | 0 | 9 | 9 | -1.21 | -1.22 |
| BOSS_W | 207 | 206 | -1 | 1 | 2 | -1.21 | -1.22 |
| CORRIDOR_OUT | 329 | 330 | 1 | 1 | 1 | 0.87 | 0.87 |
| DAIS_FRONT | 132 | 130.5 | -1.5 | 27 | 26 | -1.21 | -1.21 |
| DOOR_IN | 296 | 295 | -1 | 1 | 1 | 0.87 | 0.87 |
| NW_CORNER | 349 | 350 | 1 | 1 | 1 | -1.21 | -1.21 |
| OBELISK_N | 209 | 210 | 1 | 1 | 1 | 0.87 | 0.87 |

### Arrival radius vs detection (EAST, trials that ever enter the radius while grounded; median tick in parentheses)

| start | r=0.5 | r=1.0 | r=1.23 | r=1.5 | r=2.0 |
|---|---|---|---|---|---|
| BOSS_E | 0/30 | 30/30 (med 181) | 30/30 (med 158) | 30/30 (med 155) | 30/30 (med 151) |
| BOSS_N | 0/30 | 0/30 | 30/30 (med 222) | 30/30 (med 218) | 30/30 (med 214) |
| BOSS_S | 0/30 | 0/30 | 25/30 (med 157) | 30/30 (med 153) | 30/30 (med 149) |
| BOSS_W | 0/30 | 0/30 | 30/30 (med 211) | 30/30 (med 207) | 30/30 (med 203) |
| CORRIDOR_OUT | 0/30 | 30/30 (med 361) | 30/30 (med 332) | 30/30 (med 329) | 30/30 (med 324) |
| DAIS_FRONT | 0/30 | 0/30 | 29/30 (med 136) | 30/30 (med 132) | 30/30 (med 128) |
| DOOR_IN | 0/30 | 30/30 (med 322) | 30/30 (med 299) | 30/30 (med 296) | 30/30 (med 292) |
| NW_CORNER | 0/30 | 0/30 | 30/30 (med 352) | 30/30 (med 349) | 30/30 (med 345) |
| OBELISK_N | 0/30 | 30/30 (med 241) | 30/30 (med 212) | 30/30 (med 209) | 30/30 (med 204) |

### Where the golem rests (relative to the stop-cell centre) and its clearance to the cover (hitbox-edge to cover-edge, blocks)

| side | start | rest dx | rest dz | min gap (all ticks after arrival) | median rest gap | max drift after r=1.5 | ticks overlapping cover | fell |
|---|---|---|---|---|---|---|---|---|
| E | BOSS_E | 0.87 | 0.27 | 0.14 | 0.5 | 1.78 | 0 | 0 |
| W | BOSS_E | -0.87 | 0.27 | 0.14 | 0.5 | 1.78 | 0 | 0 |
| E | BOSS_N | -1.21 | 0.02 | 2.29 | 2.37 | 0.3 | 0 | 0 |
| W | BOSS_N | 1.22 | 0.02 | 2.29 | 2.37 | 0.3 | 0 | 0 |
| E | BOSS_S | -1.21 | 0.01 | 2.27 | 2.37 | 0.3 | 0 | 0 |
| W | BOSS_S | 1.22 | 0.01 | 2.27 | 2.38 | 0.3 | 0 | 0 |
| E | BOSS_W | -1.21 | 0.02 | 2.35 | 2.37 | 0.3 | 0 | 0 |
| W | BOSS_W | 1.22 | 0.02 | 2.35 | 2.37 | 0.3 | 0 | 0 |
| E | CORRIDOR_OUT | 0.87 | 0.31 | 0.14 | 0.61 | 1.78 | 0 | 0 |
| W | CORRIDOR_OUT | -0.87 | 0.31 | 0.14 | 0.61 | 1.78 | 0 | 0 |
| E | DAIS_FRONT | -1.21 | 0.02 | 2.36 | 2.37 | 0.16 | 0 | 0 |
| W | DAIS_FRONT | 1.21 | 0.01 | 2.36 | 2.37 | 0.16 | 0 | 0 |
| E | DOOR_IN | 0.87 | 0.27 | 0.14 | 0.5 | 1.78 | 0 | 0 |
| W | DOOR_IN | -0.87 | 0.27 | 0.14 | 0.5 | 1.78 | 0 | 0 |
| E | NW_CORNER | -1.21 | 0.01 | 2.36 | 2.36 | 0.16 | 0 | 0 |
| W | NW_CORNER | 1.21 | 0.01 | 2.36 | 2.36 | 0.16 | 0 | 0 |
| E | OBELISK_N | 0.87 | 0.3 | 0.14 | 0.61 | 1.78 | 0 | 0 |
| W | OBELISK_N | -0.87 | 0.31 | 0.14 | 0.61 | 1.78 | 0 | 0 |

### Navigation start-up and stalls (all 540 base trials)

First non-null path after spawn (ticks): {'n': 540, 'min': 20, 'median': 20.5, 'p90': 21, 'p95': 21, 'p99': 21, 'max': 21, 'mean': 20.5, 'sd': 0.5}

Longest consecutive still stretch before arrival (ticks): {'n': 540, 'min': 0, 'median': 4.0, 'p90': 11, 'p95': 12, 'p99': 14, 'max': 14, 'mean': 4.77, 'sd': 4.04}; ticks with a lost path after the first path: 1100.

### O-4 stop-cell candidates (start BOSS_S; zone = P7 cell-centre distance to cover rectangle; delta = offset from the PROPOSED Z2 stop cell)

| zone | cell dist | delta (x,z) | arrived | median tick (r=1.5) | min hitbox gap | median rest gap | median rest offset from own cell centre | overlap ticks |
|---|---|---|---|---|---|---|---|---|
| Z0 | 0.500 | 2,0 | 6/6 | 154.5 | 0 | 0 | 0.78 | 486 |
| Z0 | 0.500 | 3,0 | 6/6 | 183.5 | 0 | 0 | 1.26 | 486 |
| Z0 | 0.500 | 4,0 | 6/6 | 187.5 | 0 | 0 | 0.37 | 486 |
| Z0 | 0.500 | 5,-4 | 6/6 | 156 | 0.14 | 0.27 | 0.32 | 0 |
| Z0 | 0.500 | 5,0 | 6/6 | 194.5 | 0 | 0 | 1.31 | 486 |
| Z0 | 0.500 | 6,-1 | 6/6 | 194 | 0 | 0 | 1.29 | 486 |
| Z0 | 0.500 | 6,-2 | 6/6 | 182.5 | 0 | 0 | 1.22 | 486 |
| Z0 | 0.500 | 6,-3 | 6/6 | 168.5 | 0 | 0 | 0.8 | 486 |
| Z1 | 0.707 | 1,-4 | 6/6 | 104 | 0.12 | 0.14 | 0.15 | 0 |
| Z1 | 0.707 | 1,0 | 6/6 | 157.5 | 1.27 | 1.29 | 1.13 | 0 |
| Z1 | 0.707 | 6,-4 | 6/6 | 166 | 0.15 | 0.25 | 0.81 | 0 |
| Z1 | 0.707 | 6,0 | 6/6 | 198.5 | 0 | 0 | 0.29 | 486 |
| Z2 | 1.500 | 2,1 | 6/6 | 169.5 | 0.98 | 1.02 | 1.25 | 0 |
| Z2 | 1.500 | 3,1 | 6/6 | 191.5 | 0 | 0 | 1.22 | 486 |
| Z2 | 1.500 | 4,1 | 6/6 | 194.5 | 0 | 0 | 1.28 | 486 |
| Z2 | 1.500 | 5,-5 | 6/6 | 156 | 0.14 | 0.28 | 0.91 | 0 |
| Z2 | 1.500 | 5,1 | 6/6 | 198 | 0.16 | 0.86 | 0.4 | 0 |
| Z2 | 1.581 | 0,0 | 6/6 | 154 | 2.28 | 2.37 | 1.21 | 0 |
| Z2 | 1.581 | 1,1 | 6/6 | 158 | 1.66 | 1.7 | 1.2 | 0 |
| Z2 | 1.581 | 6,-5 | 6/6 | 161.5 | 0.15 | 0.94 | 0.23 | 0 |
| Z2 | 1.581 | 6,1 | 6/6 | 204.5 | 0 | 0 | 1.23 | 486 |
| Z3 | 2.121 | 0,-5 | 6/6 | 92 | 2.72 | 2.74 | 1.24 | 0 |
| Z3 | 2.121 | 0,1 | 6/6 | 155 | 2.53 | 2.56 | 1.23 | 0 |
| Z3 | 2.500 | 5,-6 | 6/6 | 165.5 | 3.32 | 3.33 | 1.18 | 0 |
| Z3 | 2.550 | -1,0 | 6/6 | 145 | 3.32 | 3.34 | 1.19 | 0 |
| Z3 | 2.550 | 6,-6 | 6/6 | 166 | 3.33 | 3.39 | 1.25 | 0 |
| Z4 | 2.915 | -1,-5 | 6/6 | 86.5 | 3.63 | 3.64 | 1.25 | 0 |
| Z4 | 2.915 | -1,1 | 6/6 | 140.5 | 2.55 | 3.04 | 0.78 | 0 |
| Z4 | 2.915 | 0,-6 | 6/6 | 90 | 3.37 | 3.38 | 1.28 | 0 |
| Z4 | 3.500 | 5,-7 | 6/6 | 155 | 4.3 | 4.33 | 1.18 | 0 |
| Z4 | 3.536 | -1,-6 | 6/6 | 81 | 4.07 | 4.14 | 1.26 | 0 |
| Z4 | 3.536 | -2,0 | 6/6 | 119 | 4.37 | 4.38 | 1.23 | 0 |
| Z4 | 3.536 | 6,-7 | 6/6 | 147.5 | 4.33 | 4.35 | 1.23 | 0 |

### O-6 one-variable-at-a-time sweep (EAST; baseline scale 0.5 / speed 0.25 is the `base:` row of the route table)

| start | variant | arrived | median tick | max tick | min hitbox gap | overlap ticks | lost-path ticks | max stall |
|---|---|---|---|---|---|---|---|---|
| BOSS_S | scale0.4 | 20/20 | 152 | 153 | 2.41 | 0 | 320 | 14 |
| BOSS_S | scale0.6 | 20/20 | 149 | 154 | 2.2 | 0 | 200 | 12 |
| BOSS_S | scale0.75 | 20/20 | 180 | 183 | 2.24 | 0 | 400 | 15 |
| BOSS_S | scale1.0 | 20/20 | 180 | 181 | 2.12 | 0 | 0 | 15 |
| BOSS_S | speed0.15 | 20/20 | 338 | 338 | 2.47 | 0 | 0 | 15 |
| BOSS_S | speed0.35 | 20/20 | 103 | 104 | 2.04 | 0 | 0 | 14 |
| BOSS_S | speed0.5 | 20/20 | 84 | 85 | 1.8 | 0 | 220 | 16 |
| NW_CORNER | scale0.4 | 20/20 | 348.5 | 349 | 2.42 | 0 | 0 | 2 |
| NW_CORNER | scale0.6 | 20/20 | 368.5 | 369 | 2.3 | 0 | 400 | 13 |
| NW_CORNER | scale0.75 | 20/20 | 431 | 451 | 2.29 | 0 | 0 | 15 |
| NW_CORNER | scale1.0 | 20/20 | 441 | 461 | 2.12 | 0 | 0 | 14 |
| NW_CORNER | speed0.15 | 20/20 | 892 | 924 | 2.47 | 0 | 120 | 16 |
| NW_CORNER | speed0.35 | 20/20 | 201.5 | 202 | 2.19 | 0 | 0 | 6 |
| NW_CORNER | speed0.5 | 20/20 | 153 | 154 | 1.7 | 0 | 400 | 12 |

### Server tick stretched by a busy loop (label = burn per tick) vs the unloaded reference

| load | condition | arrived | median tick | max tick | unloaded median | unloaded max | avg server tick ms | lost-path ticks | max stall |
|---|---|---|---|---|---|---|---|---|---|
| load60ms | load:BOSS_S | 24/24 | 153.5 | 155 | 153 | 155 | 65 | 400 | 14 |
| load60ms | load:DOOR_IN | 12/12 | 295.5 | 296 | 296 | 296 | 63.6 | 0 | 10 |
| load150ms | load:BOSS_S | 12/12 | 154 | 155 | 153 | 155 | 154.2 | 220 | 14 |
| load150ms | load:DOOR_IN | 6/6 | 295.5 | 296 | 296 | 296 | 153.6 | 0 | 8 |
| load250ms | load:BOSS_S | 12/12 | 154 | 155 | 153 | 155 | 254.4 | 220 | 14 |
| load250ms | load:DOOR_IN | 6/6 | 295.5 | 296 | 296 | 296 | 253.9 | 0 | 8 |

### Full pipeline in the real hall (victory -> golem -> open): final states and open tick (ticks after the valid victory)

| scenario | side | runs | final states | min | median | p90 | p95 | max | arrived-but-undetected | voxel mismatch after open |
|---|---|---|---|---|---|---|---|---|---|---|
| all_far_idle | E | 4 | {'FALLBACK_OPEN': 4} | 601 | 601 | 601 | 601 | 601 | 0 | {'12': 4} |
| chunk_unload | E | 3 | {'FALLBACK_OPEN': 3} | 601 | 601 | 601 | 601 | 601 | 0 | {'12': 3} |
| dispersed | E | 6 | {'ENTRANCE_OPEN': 6} | 155 | 155.5 | 156 | 156 | 156 | 0 | {'12': 6} |
| leader_logout | E | 6 | {'ENTRANCE_OPEN': 6} | 155 | 155.5 | 156 | 156 | 156 | 0 | {'12': 6} |
| live_state_loss | E | 4 | {'FALLBACK_OPEN': 4} | 601 | 601 | 601 | 601 | 601 | 4 | {'12': 4} |
| race | E | 12 | {'ENTRANCE_OPEN': 8, 'FALLBACK_OPEN': 4} | 509 | 595 | 601 | 601 | 601 | 0 | {'12': 12} |
| sampled | E | 32 | {'ENTRANCE_OPEN': 32} | 149 | 208.5 | 385 | 402 | 404 | 0 | {'12': 32} |
| sampled | W | 32 | {'ENTRANCE_OPEN': 32} | 149 | 208.5 | 385 | 401 | 404 | 0 | {'12': 32} |

### Fallback-vs-arrival race (fallback 600 ticks TEST-ONLY; golem speed set so the walk takes ~500-700 ticks)

| golem speed attr | final state | open tick | golem dist to stop | golem gap to cover | voxel mismatch |
|---|---|---|---|---|---|
| 0.1200 | ENTRANCE_OPEN | 509 | 1.456 | 2.610 | 12 |
| 0.1150 | ENTRANCE_OPEN | 552 | 1.465 | 2.619 | 12 |
| 0.1120 | ENTRANCE_OPEN | 573 | 1.461 | 2.615 | 12 |
| 0.1110 | ENTRANCE_OPEN | 574 | 1.456 | 2.609 | 12 |
| 0.1105 | ENTRANCE_OPEN | 594 | 1.449 | 2.602 | 12 |
| 0.1095 | ENTRANCE_OPEN | 595 | 1.450 | 2.604 | 12 |
| 0.1090 | ENTRANCE_OPEN | 595 | 1.464 | 2.618 | 12 |
| 0.1100 | ENTRANCE_OPEN | 599 | 1.463 | 2.616 | 12 |
| 0.1080 | FALLBACK_OPEN | 601 | 1.953 | 3.105 | 12 |
| 0.1060 | FALLBACK_OPEN | 601 | 2.510 | 3.662 | 12 |
| 0.1040 | FALLBACK_OPEN | 601 | 2.973 | 4.123 | 12 |
| 0.1000 | FALLBACK_OPEN | 601 | 3.069 | 4.162 | 12 |

### Cover opening with occupants (proxies; drop in blocks)

| side | occupant | dx | dz | drop | stayed on cell | in wall | hp before | hp after | alive |
|---|---|---|---|---|---|---|---|---|---|
| E | armor_stand | 12 | 0 | 1.000 | true | false | 0.0 | 20.0 | true |
| E | villager | 12 | 0 | 1.000 | true | false | 20.0 | 20.0 | true |
| E | armor_stand | 12 | 1 | 1.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 12 | 2 | 1.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 13 | 0 | 2.000 | true | false | 0.0 | 20.0 | true |
| E | villager | 13 | 0 | 2.000 | true | false | 20.0 | 20.0 | true |
| E | armor_stand | 13 | 1 | 2.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 13 | 2 | 2.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 14 | 0 | 3.000 | true | false | 0.0 | 20.0 | true |
| E | villager | 14 | 0 | 2.000 | true | false | 20.0 | 20.0 | true |
| E | armor_stand | 14 | 1 | 3.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 14 | 2 | 3.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 15 | 0 | 4.000 | true | false | 0.0 | 20.0 | true |
| E | villager | 15 | 0 | 1.751 | false | false | 20.0 | 19.0 | true |
| E | armor_stand | 15 | 1 | 4.000 | true | false | 0.0 | 20.0 | true |
| E | armor_stand | 15 | 2 | 4.000 | true | false | 0.0 | 20.0 | true |
| E | mock_player | 13 | 2 | 2.000 | true | false | 20.0 | 20.0 | true |
| E | mock_player_deepest_column | 15 | 0 | 4.000 | true | false | 20.0 | 20.0 | true |
| E | gold_golem_on_cover | 12 | 0 | 1.000 | true | false | 20.0 | 20.0 | true |
| E | gold_golem_at_stop | 10 | 3 | 0.000 | true | false | 20.0 | 20.0 | true |
| W | armor_stand | -12 | 0 | 1.000 | true | false | 0.0 | 20.0 | true |
| W | villager | -12 | 0 | 1.000 | false | false | 20.0 | 20.0 | true |
| W | armor_stand | -12 | 1 | 1.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -12 | 2 | 1.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -13 | 0 | 2.000 | true | false | 0.0 | 20.0 | true |
| W | villager | -13 | 0 | 2.000 | true | false | 20.0 | 20.0 | true |
| W | armor_stand | -13 | 1 | 2.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -13 | 2 | 2.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -14 | 0 | 3.000 | true | false | 0.0 | 20.0 | true |
| W | villager | -14 | 0 | 2.580 | true | false | 20.0 | 20.0 | true |
| W | armor_stand | -14 | 1 | 3.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -14 | 2 | 3.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -15 | 0 | 4.000 | true | false | 0.0 | 20.0 | true |
| W | villager | -15 | 0 | 1.580 | false | false | 20.0 | 19.0 | true |
| W | armor_stand | -15 | 1 | 4.000 | true | false | 0.0 | 20.0 | true |
| W | armor_stand | -15 | 2 | 4.000 | true | false | 0.0 | 20.0 | true |
| W | mock_player | -13 | 2 | 2.000 | true | false | 20.0 | 20.0 | true |
| W | mock_player_deepest_column | -15 | 0 | 4.000 | true | false | 20.0 | 20.0 | true |
| W | gold_golem_on_cover | -12 | 0 | 1.000 | true | false | 20.0 | 20.0 | true |
| W | gold_golem_at_stop | -10 | 3 | 0.000 | true | false | 20.0 | 20.0 | true |

### After opening: golem walk from the stop point (reached tick, -1 = not within 1500 ticks)

| side | golem -> goal | reached tick |
|---|---|---|
| E | scale0.5->HATCH | 16 |
| E | scale0.5->VAULT_ENTRY | 214 |
| E | scale0.75->HATCH | 16 |
| E | scale0.75->VAULT_ENTRY | -1 |
| E | scale1.0->HATCH | 16 |
| E | scale1.0->VAULT_ENTRY | -1 |
| W | scale0.5->HATCH | 16 |
| W | scale0.5->VAULT_ENTRY | 214 |
| W | scale0.75->HATCH | 17 |
| W | scale0.75->VAULT_ENTRY | -1 |
| W | scale1.0->HATCH | 17 |
| W | scale1.0->VAULT_ENTRY | -1 |

### O-6: passing the real 3x3 door from outside (start CORRIDOR_OUT, speed 0.25)

| scale | arrived at the stop point | median tick | hitbox height | hitbox width |
|---|---|---|---|---|
| 0.5 | 6/6 | 329 | 1.35 | 0.7 |
| 0.75 | 6/6 | 355.5 | 2.03 | 1.05 |
| 1.0 | 6/6 | 361 | 2.7 | 1.4 |
| 1.1 | 2/6 | 382.5 | 2.97 | 1.54 |
| 1.2 | 0/6 | - | 3.24 | 1.68 |
| 1.5 | 0/6 | - | 4.05 | 2.1 |

### Golem arriving beside the cover with the cover REMOVED at the first arrival (golems keep moving for 80 ticks)

| side | condition | arrived | fell into the opening | min hitbox gap | overlap ticks | max |final dy| |
|---|---|---|---|---|---|---|
| E | scale0.5:BOSS_E | 6/6 | 0 | 0.14 | 0 | 0 |
| E | scale0.5:BOSS_S | 6/6 | 0 | 2.27 | 0 | 0 |
| E | scale0.5:DOOR_IN | 6/6 | 0 | 0.14 | 0 | 0 |
| E | scale0.5:OBELISK_N | 6/6 | 0 | 0.14 | 0 | 0 |
| E | scale0.75:BOSS_E | 6/6 | 0 | 2.31 | 0 | 0 |
| E | scale1.0:BOSS_E | 6/6 | 0 | 2.19 | 0 | 0 |
| W | scale0.5:BOSS_E | 6/6 | 0 | 0.14 | 0 | 0 |
| W | scale0.5:BOSS_S | 6/6 | 0 | 2.28 | 0 | 0 |
| W | scale0.5:DOOR_IN | 6/6 | 0 | 0.14 | 0 | 0 |

### O-4 stop-cell candidates approached from BOTH directions (west approach = start BOSS_S, n=6; east-aisle approach = start DOOR_IN, n=4)

| zone | cell dist | delta (x,z) | BOSS_S arrived | BOSS_S min gap | DOOR_IN arrived | DOOR_IN min gap | overlap ticks (both) | BOSS_S median tick | DOOR_IN median tick |
|---|---|---|---|---|---|---|---|---|---|
| Z0 | 0.500 | 2,0 | 6/6 | 0 | 4/4 | 0 | 810 | 154.5 | 293.5 |
| Z0 | 0.500 | 3,0 | 6/6 | 0 | 4/4 | 0 | 810 | 183.5 | 297.5 |
| Z0 | 0.500 | 4,0 | 6/6 | 0 | 4/4 | 0 | 810 | 187.5 | 328.5 |
| Z0 | 0.500 | 5,-4 | 6/6 | 0.14 | 4/4 | 1.29 | 0 | 156 | 282.5 |
| Z0 | 0.500 | 5,0 | 6/6 | 0 | 4/4 | 0 | 810 | 194.5 | 302.5 |
| Z0 | 0.500 | 6,-1 | 6/6 | 0 | 4/4 | 0 | 810 | 194 | 303 |
| Z0 | 0.500 | 6,-2 | 6/6 | 0 | 4/4 | 0 | 810 | 182.5 | 288.5 |
| Z0 | 0.500 | 6,-3 | 6/6 | 0 | 4/4 | 0.36 | 486 | 168.5 | 284.5 |
| Z1 | 0.707 | 1,-4 | 6/6 | 0.12 | 4/4 | 0.11 | 0 | 104 | 241 |
| Z1 | 0.707 | 1,0 | 6/6 | 1.27 | 4/4 | 0.14 | 0 | 157.5 | 291.5 |
| Z1 | 0.707 | 6,-4 | 6/6 | 0.15 | 4/4 | 1.35 | 0 | 166 | 273 |
| Z1 | 0.707 | 6,0 | 6/6 | 0 | 4/4 | 0 | 810 | 198.5 | 306.5 |
| Z2 | 1.500 | 2,1 | 6/6 | 0.98 | 4/4 | 0 | 12 | 169.5 | 300.5 |
| Z2 | 1.500 | 3,1 | 6/6 | 0 | 4/4 | 0 | 810 | 191.5 | 305.5 |
| Z2 | 1.500 | 4,1 | 6/6 | 0 | 4/4 | 0 | 810 | 194.5 | 314.5 |
| Z2 | 1.500 | 5,-5 | 6/6 | 0.14 | 4/4 | 2.26 | 0 | 156 | 268 |
| Z2 | 1.500 | 5,1 | 6/6 | 0.16 | 4/4 | 0.18 | 0 | 198 | 308 |
| Z2 | 1.581 | 0,0 | 6/6 | 2.28 | 4/4 | 0.14 | 0 | 154 | 295.5 |
| Z2 | 1.581 | 1,1 | 6/6 | 1.66 | 4/4 | 0.15 | 0 | 158 | 298.5 |
| Z2 | 1.581 | 6,-5 | 6/6 | 0.15 | 4/4 | 2.38 | 0 | 161.5 | 270.5 |
| Z2 | 1.581 | 6,1 | 6/6 | 0 | 4/4 | 0 | 648 | 204.5 | 309.5 |
| Z3 | 2.121 | 0,-5 | 6/6 | 2.72 | 4/4 | 2.7 | 0 | 92 | 225.5 |
| Z3 | 2.121 | 0,1 | 6/6 | 2.53 | 4/4 | 0.15 | 0 | 155 | 308.5 |
| Z3 | 2.500 | 5,-6 | 6/6 | 3.32 | 4/4 | 3.3 | 0 | 165.5 | 260 |
| Z3 | 2.550 | -1,0 | 6/6 | 3.32 | 4/4 | 3.34 | 0 | 145 | 314.5 |
| Z3 | 2.550 | 6,-6 | 6/6 | 3.33 | 4/4 | 3.35 | 0 | 166 | 262.5 |
| Z4 | 2.915 | -1,-5 | 6/6 | 3.63 | 4/4 | 3.36 | 0 | 86.5 | 226 |
| Z4 | 2.915 | -1,1 | 6/6 | 2.55 | 4/4 | 3.52 | 0 | 140.5 | 313.5 |
| Z4 | 2.915 | 0,-6 | 6/6 | 3.37 | 4/4 | 3.6 | 0 | 90 | 218 |
| Z4 | 3.500 | 5,-7 | 6/6 | 4.3 | 4/4 | 4.27 | 0 | 155 | 253 |
| Z4 | 3.536 | -1,-6 | 6/6 | 4.07 | 4/4 | 4.07 | 0 | 81 | 216.5 |
| Z4 | 3.536 | -2,0 | 6/6 | 4.37 | 4/4 | 4.3 | 0 | 119 | 289 |
| Z4 | 3.536 | 6,-7 | 6/6 | 4.33 | 4/4 | 4.37 | 0 | 147.5 | 249.5 |

Cells that arrived in every trial from both starts, never overlapped the cover and kept a hitbox gap >= 1.0 block: Z3 d=2.121 delta=(0,-5) gap>=2.70; Z3 d=2.500 delta=(5,-6) gap>=3.30; Z3 d=2.550 delta=(-1,0) gap>=3.32; Z3 d=2.550 delta=(6,-6) gap>=3.33; Z4 d=2.915 delta=(-1,-5) gap>=3.36; Z4 d=2.915 delta=(-1,1) gap>=2.55; Z4 d=2.915 delta=(0,-6) gap>=3.37; Z4 d=3.500 delta=(5,-7) gap>=4.27; Z4 d=3.536 delta=(-1,-6) gap>=4.07; Z4 d=3.536 delta=(-2,0) gap>=4.30; Z4 d=3.536 delta=(6,-7) gap>=4.33
