"""Actual W-key balcony entry/return and guard push, eight independent scenarios."""
import os,subprocess,time,math,re,json,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('c',H/'control.py');c=importlib.util.module_from_spec(s);s.loader.exec_module(c)
env=dict(os.environ,DISPLAY=':88',LD_LIBRARY_PATH='/tmp/fullbuild-mc/x11/usr/lib/x86_64-linux-gnu');X='/tmp/fullbuild-mc/x11/usr/bin/xdotool'
def key(a):subprocess.run([X,a,'w'],env=env,check=True)
results=[]
with c.rc.connect() as sock:
 n=1
 def cmd(t):
  global n
  n+=1;c.rc.send_packet(sock,n,2,t);return c.rc.read_packet(sock)[2]
 def pos():return [float(v) for v in re.findall(r'(-?[\d.]+)d',cmd('data get entity FullBuilder Pos'))]
 cmd('gamemode adventure FullBuilder')
 for i,b in enumerate(json.loads((H/'model.json').read_text())['balconies']):
  sign=b['sign'];Y=b['top']+65;start=[1000.5,Y,1000.5+sign*17.5];cmd('tp FullBuilder '+' '.join(map(str,start)));time.sleep(1)
  trace=[];statuses=[]
  for label,target,timeout in [('in',1000.5+sign*22,8),('push',1000.5+sign*27,3),('out',start[2],8)]:
   began=time.monotonic();status='TIMEOUT';key('keydown')
   try:
    while time.monotonic()-began<timeout:
     p=pos();trace.append([label,*p]);yaw=math.degrees(math.atan2(-(1000.5-p[0]),target-p[2]));cmd(f'rotate FullBuilder {yaw} 5')
     if math.hypot(1000.5-p[0],target-p[2])<.45 and abs(p[1]-Y)<.2:status='REACHED';break
     if abs(p[1]-Y)>1:status='HEIGHT_FAILURE';break
     time.sleep(.12)
   finally:key('keyup')
   statuses.append([label,status])
  safe=all(abs(p[2]-Y)<.3 for p in trace);results.append({'balcony':i+1,'top':Y,'statuses':statuses,'stable_height':safe,'trace':trace})
(H/'live_balconies.json').write_text(json.dumps(results,separators=(',',':'))+'\n');print([{k:v for k,v in r.items() if k!='trace'} for r in results])
