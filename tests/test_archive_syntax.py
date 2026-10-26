import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
from validate_archive import DEFAULT, MAX_JSON_BYTES, MAX_JSON_DEPTH, parse_document, validate


class ArchiveSyntaxTests(unittest.TestCase):
    def test_real_fixture_still_uses_the_same_semantic_model(self):
        self.assertEqual(5, validate(parse_document(DEFAULT.read_bytes()))['events'])

    def test_requires_complete_single_object(self):
        for payload in (b'{} trailing', b'{} {}', b'{}[]', b'[]', b'null', b'{}\0'):
            with self.subTest(payload=payload), self.assertRaises(ValueError):
                parse_document(payload)

    def test_decoded_keys_must_be_unique_at_every_object(self):
        for payload in (b'{"a":1,"a":2}', b'{"a":1,"\\u0061":2}', b'{"x":{"a":1,"a":2}}'):
            with self.subTest(payload=payload), self.assertRaises(ValueError):
                parse_document(payload)
        self.assertEqual({'x': {'a': 1}, 'y': {'a': 2}}, parse_document(b'{"x":{"a":1},"y":{"a":2}}'))

    def test_rejects_lenient_extensions(self):
        for payload in (b'/*comment*/{}', b"{'a':1}", b'{a:1}', b'{"a":+1}', b'{"a":01}', b'{"a":NaN}', b'{"a":Infinity}'):
            with self.subTest(payload=payload), self.assertRaises(ValueError):
                parse_document(payload)

    def test_bounds_nesting_before_the_recursive_json_loader(self):
        def nested(depth):
            return b'{"x":' + b'[' * (depth - 1) + b'0' + b']' * (depth - 1) + b'}'
        self.assertIsInstance(parse_document(nested(MAX_JSON_DEPTH)), dict)
        for depth in (MAX_JSON_DEPTH + 1, 4096):
            with self.subTest(depth=depth), self.assertRaisesRegex(ValueError, 'nesting limit'):
                parse_document(nested(depth))

    def test_delimiters_and_escaped_quotes_in_strings_do_not_consume_depth(self):
        value = {'x': '[{}]"' * 4096}
        self.assertEqual(value, parse_document(json.dumps(value).encode('utf-8')))

    def test_bounds_bytes_including_unicode_and_whitespace(self):
        payload = b'{}' + b' ' * (MAX_JSON_BYTES - 2)
        self.assertEqual({}, parse_document(payload))
        for invalid in (payload + b' ', ('{"x":"' + 'é' * 600000 + '"}').encode('utf-8')):
            with self.assertRaises(ValueError):
                parse_document(invalid)

    def test_rejects_invalid_utf8_and_unpaired_unicode_escapes(self):
        for invalid in (b'{"x":"\xff"}', b'{"x":"\\ud800"}', b'{"x":"\\udc00"}'):
            with self.assertRaises(ValueError):
                parse_document(invalid)
        self.assertEqual({'x': '😀'}, parse_document(b'{"x":"\\ud83d\\ude00"}'))
