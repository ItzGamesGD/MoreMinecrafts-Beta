"""Isolated-world vanilla spawner and vault interaction smoke check; not full encounter balance."""
import importlib.util,json,time,os,subprocess
from pathlib import Path
H=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('c',H/'control.py');c=importlib.util.module_from_spec(s);s.loader.exec_module(c)
out={};out['prepare']=c.commands(['gamemode adventure FullBuilder','effect give FullBuilder minecraft:resistance 120 255 true','difficulty normal','tp FullBuilder 1000.5 76 1000.5 0 0'])
time.sleep(25)
out['spawner_active']=c.commands(['execute if block 993 77 993 minecraft:trial_spawner[trial_spawner_state=active]','execute if entity @e[type=minecraft:zombie]','data get block 993 77 993'])
out['vault_prepare']=c.commands(['difficulty peaceful','tp FullBuilder 994.3 76 1009.5 90 32','item replace entity FullBuilder weapon.mainhand with minecraft:trial_key']);time.sleep(2)
env=dict(os.environ,DISPLAY=':88',LD_LIBRARY_PATH='/tmp/fullbuild-mc/x11/usr/lib/x86_64-linux-gnu');subprocess.run(['/tmp/fullbuild-mc/x11/usr/bin/xdotool','click','3'],env=env,check=True)
time.sleep(5)
out['vault_after']=c.commands(['data get entity FullBuilder SelectedItem','data get entity FullBuilder Inventory','data get block 992 76 1009','execute positioned 992.5 76.5 1009.5 if entity @e[type=minecraft:item,distance=..5]'])
out['scope']='real nearby player triggers trial spawner; one real-client trial-key click on one vault. Not all floors/full waves/ominous/restart/multiplayer validation.'
(H/'functional_smoke.json').write_text(json.dumps(out,indent=1)+'\n');print(json.dumps(out,indent=1))
