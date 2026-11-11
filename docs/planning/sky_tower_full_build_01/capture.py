"""Capture real Minecraft frames via X11. Never renders substitutes."""
import importlib.util,time,json
from pathlib import Path
from PIL import ImageGrab
H=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('c',H/'control.py');c=importlib.util.module_from_spec(s);s.loader.exec_module(c)
shots=[('01_front',[1068.5,112,1000.5,90,8],6000),('02_diagonal',[1057,113,1057,135,10],6000),('03_looking_up',[1032,68,1000.5,90,-58],6000),('04_arcade',[1028,93,1010,110,5],6000),('05_balcony',[1014,90,1034,160,20],6000),('06_battle_interior',[1009,79,1000.5,90,12],6000),('07_gate',[1020,90,1000.5,90,-12],6000),('08_sanctum',[1018,141,1018,135,28],6000),('09_night',[1057,113,1057,135,10],18000),('10_distant',[1090,120,1090,135,10],6000)]
(H/'screenshots').mkdir(exist_ok=True);c.commands(['gamemode spectator FullBuilder']);log=[]
for name,pose,t in shots:
 c.commands(['time set '+str(t),'tp FullBuilder '+' '.join(map(str,pose))]);time.sleep(3)
 ImageGrab.grab(xdisplay=':88').save(H/'screenshots'/f'{name}.png');log.append({'name':name,'pose':pose,'time':t,'source':'actual Minecraft 26.3 X11 client frame'})
(H/'captures.json').write_text(json.dumps(log,indent=1)+'\n');print('real screenshots',len(log))
