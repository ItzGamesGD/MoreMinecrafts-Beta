"""Small isolated-server command runner. SERVER_DIR points at temporary server, not credentials in repo."""
import os,sys,importlib.util
from pathlib import Path
H=Path(__file__).resolve().parent;S=Path(os.environ.get('SERVER_DIR','/tmp/fullbuild-mc'));p=dict(l.split('=',1) for l in (S/'server.properties').read_text().splitlines() if '=' in l);os.environ['RCON_PASSWORD']=p['rcon.password'];os.environ['MC_RCON_PORT']=p['rcon.port']
s=importlib.util.spec_from_file_location('rc',H.parents[2]/'scripts/arena_validation/rcon.py');rc=importlib.util.module_from_spec(s);s.loader.exec_module(rc)
def commands(cmds):
 with rc.connect() as sock:
  sock.settimeout(60);out=[]
  for i,cmd in enumerate(cmds,2):rc.send_packet(sock,i,2,cmd);out.append(rc.read_packet(sock)[2])
 return out
if __name__=='__main__':
 for r in commands(sys.argv[1:]):print(r)
