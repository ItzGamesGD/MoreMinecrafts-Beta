"""Full-fidelity Google Drive mirror of every Git ref of a GitHub repository.

See tools/drive_mirror/README.md for the design, Drive layout and runbook.
The package only reads from Git/GitHub; it never writes to the source repository.
"""

SCHEMA_VERSION = 1
