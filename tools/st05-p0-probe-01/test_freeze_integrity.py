"""Integrity dry-run in disposable directories only; never freezes anything."""
import importlib.util
from pathlib import Path
import shutil
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('integrity', Path(__file__).with_name('freeze_integrity.py'))
F = importlib.util.module_from_spec(spec)
spec.loader.exec_module(F)


class Integrity(unittest.TestCase):
    def test_exact_inventory_and_tamper_rejection(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            for name in F.EXECUTION + F.PROVENANCE:
                dest = root / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(F.ROOT / name, dest)
            F.prepare(root)
            anchor = F.sha(root / F.INVENTORY)
            self.assertEqual(F.verify(root, anchor), len(F.EXECUTION + F.PROVENANCE))
            path = root / F.EXECUTION[0]
            original = path.read_bytes()
            for bad in ('missing', 'altered', 'extra', 'manifest'):
                extra = root / F.BASE / 'extra'
                manifest = root / F.INVENTORY
                before = manifest.read_bytes()
                if bad == 'missing': path.unlink()
                if bad == 'altered': path.write_bytes(original + b' ')
                if bad == 'extra': extra.write_text('unexpected')
                if bad == 'manifest': manifest.write_bytes(before + b' ')
                with self.assertRaises((ValueError, OSError)):
                    F.verify(root, anchor)
                path.write_bytes(original)
                extra.unlink(missing_ok=True)
                manifest.write_bytes(before)
            linked = root / F.BASE / 'linked'
            linked.symlink_to(path)
            with self.assertRaises(ValueError):
                F.verify(root, anchor)

    def test_unprotected_execution_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            marker = Path(td) / 'must-not-execute'
            with self.assertRaises(ValueError):
                F.protected(Path(td), '0' * 64, ['touch', str(marker)])
            self.assertFalse(marker.exists())


if __name__ == '__main__':
    unittest.main()
