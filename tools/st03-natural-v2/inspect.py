import json,os,subprocess,zipfile
from pathlib import Path
out=Path('build/st03-natural/inspect');out.mkdir(parents=True,exist_ok=True)
for jar in (Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/fabric-loom').rglob('*.jar'):
 try:
  with zipfile.ZipFile(jar) as z:
   if 'net/minecraft/world/level/levelgen/structure/Structure.class' not in z.namelist():continue
  text=subprocess.run(['javap','-p','-c','-classpath',str(jar),'net.minecraft.world.level.levelgen.structure.Structure'],capture_output=True,text=True).stdout
  if text:(out/'structure-api.txt').write_text(text);break
 except (OSError,zipfile.BadZipFile):pass
