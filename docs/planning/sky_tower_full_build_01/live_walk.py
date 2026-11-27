"""Real client W-key movement; server rotation only, no teleport after initial placement."""
import os,math,time,re,json,subprocess,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent
s=importlib.util.spec_from_file_location('ctl',H/'control.py');c=importlib.util.module_from_spec(s);s.loader.exec_module(c)
XDO='/tmp/fullbuild-mc/x11/usr/bin/xdotool';env=dict(os.environ,DISPLAY=':88',LD_LIBRARY_PATH='/tmp/fullbuild-mc/x11/usr/lib/x86_64-linux-gnu')
def key(action):subprocess.run([XDO,action,'w'],env=env,check=True)
# Long route visits the centre of every floor, not merely passing its elevation outside.
wps=[]
for L in range(5):
 for deg in range(3,360,3):
  a=math.radians(deg);wps.append([1000.5+17.5*math.cos(a),1000.5+17.5*math.sin(a),None])
 wps.append([1000.5,1000.5,65+[11,22,34,48,65][L]])
 wps.append([1017.8,1000.5,None])
wps=wps[:-1]
c.commands(['gamemode adventure FullBuilder','effect clear FullBuilder','tp FullBuilder 1023.375 66 1000.375 90 0']);time.sleep(2)
trace=[];events=[];k=0;result='TIMEOUT';t0=time.monotonic();last=t0
with c.rc.connect() as sock:
 n=1
 def cmd(t):
  global n
  n+=1;c.rc.send_packet(sock,n,2,t);return c.rc.read_packet(sock)[2]
 key('keydown')
 try:
  while time.monotonic()-t0<320:
   raw=cmd('data get entity FullBuilder Pos');p=[float(v) for v in re.findall(r'(-?[\d.]+)d',raw)]
   if len(p)!=3:raise RuntimeError(raw)
   now=time.monotonic();trace.append([round(now-t0,3),*p,k])
   while k<len(wps) and math.hypot(wps[k][0]-p[0],wps[k][1]-p[2])<.75:
    if wps[k][2] is not None:
     if abs(p[1]-wps[k][2])>.6:break
     events.append({'target':k,'y':p[1],'time':round(now-t0,2)})
    k+=1;last=now
   if k==len(wps):result='REACHED';break
   if p[1]<64:result='FELL';break
   if now-last>10:result='STUCK';break
   tx,tz,_=wps[k];yaw=math.degrees(math.atan2(-(tx-p[0]),tz-p[2]));cmd(f'rotate FullBuilder {yaw:.3f} 5');time.sleep(.12)
 finally:key('keyup')
out={'result':result,'trace':trace,'floor_visits':events,'waypoint':k,'waypoints':len(wps),'seconds':round(time.monotonic()-t0,2),'jump_or_sprint':False,'mode':'real 26.3 client holds W; initial teleport only; RCON rotates and samples position'}
(H/'live_walk.json').write_text(json.dumps(out,separators=(',',':'))+'\n');print({k:v for k,v in out.items() if k!='trace'})
