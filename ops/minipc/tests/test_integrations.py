import io
from email.message import Message
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
from urllib.error import HTTPError

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from integrations import request


class IntegrationErrorSafety(unittest.TestCase):
    def error(self, retry_after):
        headers = Message()
        headers['Retry-After'] = retry_after
        return HTTPError('https://api.telegram.org/botSECRET/sendMessage', 429,
                         'SECRET', headers, io.BytesIO(b'SECRET response'))

    def test_untrusted_retry_after_is_not_propagated(self):
        for value in ('SECRET', 'https://example.test/SECRET', '-1', '999999999999'):
            with self.subTest(value=value), patch('urllib.request.urlopen', side_effect=self.error(value)):
                with self.assertRaisesRegex(RuntimeError, 'Retry-After: not provided') as raised:
                    request('https://api.telegram.org/botSECRET/sendMessage')
                self.assertNotIn('SECRET', str(raised.exception))
                self.assertTrue(raised.exception.__suppress_context__)

    def test_numeric_retry_after_is_preserved(self):
        with patch('urllib.request.urlopen', side_effect=self.error('30')):
            with self.assertRaisesRegex(RuntimeError, 'Retry-After: 30$'):
                request('https://api.telegram.org/botSECRET/sendMessage')


if __name__ == '__main__':
    unittest.main()
