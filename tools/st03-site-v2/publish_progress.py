import json
import os
from pathlib import Path
import subprocess
import sys
import time

folder=Path(sys.argv[1])
while True:
    p=folder/'progress.json'
    if p.exists():
        try:
            data=json.loads(p.read_text())
            payload={'name':f"st03-v2-progress-{data['seed']}", 'head_sha':os.environ['GITHUB_SHA'],
                     'status':'completed', 'conclusion':'neutral',
                     'output':{'title':'ST03 V2 measurement progress','summary':json.dumps(data)}}
            subprocess.run(['gh','api','-X','POST',f"repos/{os.environ['GITHUB_REPOSITORY']}/check-runs",'--input','-'],
                           input=json.dumps(payload),text=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        except (ValueError, OSError):
            pass
    time.sleep(45)
