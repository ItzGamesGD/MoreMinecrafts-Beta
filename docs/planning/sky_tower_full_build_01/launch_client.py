import os,subprocess
from pathlib import Path
h=Path('/tmp/fullbuild-mc');(h/'game').mkdir(exist_ok=True);(h/'game/options.txt').write_text('autoJump:false\ntutorialStep:none\nrenderDistance:10\nsimulationDistance:3\nguiScale:2\nmaxFps:30\nenableVsync:false\nskipMultiplayerWarning:true\nonboardAccessibility:false\n')
env=dict(os.environ);env.update(DISPLAY=':88',LIBGL_ALWAYS_SOFTWARE='1',LD_LIBRARY_PATH='/tmp/fullbuild-mc/x11/usr/lib/x86_64-linux-gnu',VK_ICD_FILENAMES='/tmp/fullbuild-mc/x11/usr/share/vulkan/icd.d/lvp_icd.json',XDG_RUNTIME_DIR='/tmp/fullbuild-mc/runtime')
args=[os.environ.get('JAVA_BIN','/workspace/toolchains/jdk-25.0.4.1/bin/java'),'-Xmx2G','--enable-native-access=ALL-UNNAMED','-cp',(h/'classpath.txt').read_text(),'net.minecraft.client.main.Main','--username','FullBuilder','--version','26.3','--gameDir',str(h/'game'),'--assetsDir',str(h/'assets'),'--assetIndex','34','--accessToken','0','--uuid','00000000000000000000000000000001','--userType','legacy','--versionType','release','--quickPlayMultiplayer','127.0.0.1:25585','--width','1280','--height','720']
with (h/'client.log').open('w') as f:subprocess.run(args,env=env,stdout=f,stderr=subprocess.STDOUT)
