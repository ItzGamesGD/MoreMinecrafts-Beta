"""ST-01 P2 WEST candidate: exact X-mirror of the east candidate and of the P1 context (E1 exit toward -X).
Kept as a separate module/entry so that the west model is built and verified independently of the east run."""
from st01_p2_model_east import make_east, mirror_model
def make_west(): return mirror_model(make_east())
