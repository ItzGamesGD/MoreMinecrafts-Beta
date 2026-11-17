import json,os,subprocess,zipfile
from pathlib import Path
out=Path('build/st03-natural/inspect');out.mkdir(parents=True,exist_ok=True)
for jar in (Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/fabric-loom').rglob('*.jar'):
 try:
  with zipfile.ZipFile(jar) as z:
   if 'net/minecraft/world/level/levelgen/structure/Structure.class' not in z.namelist():continue
  text=subprocess.run(['javap','-p','-c','-classpath',str(jar),'net.minecraft.world.level.levelgen.structure.Structure'],capture_output=True,text=True).stdout
  if text:
   (out/'structure-api.txt').write_text(text)
   refs=subprocess.run(['javap','-p','-c','-classpath',str(jar),'net.minecraft.world.level.chunk.ChunkGenerator'],capture_output=True,text=True).stdout
   (out/'chunk-generator-api.txt').write_text(refs)
   diagnostics=[]
   for cls in ['net.minecraft.server.MinecraftServer','net.minecraft.server.level.ServerLevel','net.minecraft.world.level.Level','net.minecraft.world.entity.Mob','net.minecraft.world.entity.monster.Monster']:
    api=subprocess.run(['javap','-p','-c','-classpath',str(jar),cls],capture_output=True,text=True).stdout
    lines=api.splitlines();starts=[i for i,line in enumerate(lines) if line.startswith('  ') and not line.startswith('    ') and '(' in line]
    for n,i in enumerate(starts):
     if any(word in lines[i] for word in ['setDifficulty','SpawnSettings','checkDespawn','shouldDespawnInPeaceful']):
      end=starts[n+1] if n+1<len(starts) else len(lines)
      diagnostics.append(cls+'\n'+'\n'.join(lines[i:end]))
   (out/'difficulty-api.txt').write_text('\n\n'.join(diagnostics))
   break
 except (OSError,zipfile.BadZipFile):pass
