"""Exercise the real subprocess boundary used by LG's delayed pairing prompt."""
import sys
import unittest

from setup_app.core import SetupError
from setup_app.native import Runner


class NativePromptTests(unittest.TestCase):
    def test_secret_waits_for_complete_delayed_prompt_and_is_redacted(self):
        child = r'''
import queue, sys, threading, time
received = queue.Queue()
threading.Thread(target=lambda: received.put(sys.stdin.buffer.read(1)), daemon=True).start()
time.sleep(.15)
if not received.empty():
    sys.exit(7)
sys.stdout.write('input pass'); sys.stdout.flush()
time.sleep(.05)
if not received.empty():
    sys.exit(8)
sys.stdout.write('phrase:'); sys.stdout.flush()
first = received.get(timeout=2)
value = (first + sys.stdin.buffer.read()).decode().strip()
if value != 'Test42':
    sys.exit(9)
print('Registered ' + value)
'''
        out = Runner()([sys.executable, '-c', child], stdin='Test42\n',
                       stdin_prompt='input passphrase:', timeout=5)
        self.assertIn('Registered', out)
        self.assertNotIn('Test42', out)

    def test_successful_exit_without_prompt_does_not_count_as_pairing(self):
        with self.assertRaises(SetupError):
            Runner()([sys.executable, '-c', "print('Key downloaded only')"],
                     stdin='Test42\n', stdin_prompt='input passphrase:', timeout=3)

    def test_unresponsive_prompt_is_bounded(self):
        with self.assertRaises(SetupError):
            Runner()([sys.executable, '-c', 'import time; time.sleep(5)'],
                     stdin='Test42\n', stdin_prompt='input passphrase:', timeout=.2)

    def test_large_vendor_output_is_rejected_before_sending_secret(self):
        with self.assertRaises(SetupError):
            Runner()([sys.executable, '-c', "import sys; sys.stdout.write('x'*1100000)"],
                     stdin='Test42\n', stdin_prompt='input passphrase:', timeout=5)


if __name__ == '__main__':
    unittest.main()
