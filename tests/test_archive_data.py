import copy
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
from validate_archive import DEFAULT, validate


class ArchiveDataTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(DEFAULT.read_text())

    def invalid(self, mutation):
        mutation(self.data)
        with self.assertRaises((ValueError, TypeError)):
            validate(self.data)

    def test_bundled_demo_and_edge_outcomes_are_complete(self):
        result = validate(self.data)
        self.assertEqual((5, 8, 14, 1), tuple(result[k] for k in ('events', 'fighters', 'bouts', 'corrections')))
        outcomes = {b['result'] for e in self.data['events'] for b in e['bouts']}
        self.assertEqual({'red_win', 'blue_win', 'draw', 'no_contest', 'unknown', 'not_applicable'}, outcomes)

    def test_duplicate_ids_rejected(self):
        self.invalid(lambda d: d['fighters'].append(copy.deepcopy(d['fighters'][0])))

    def test_unknown_fighter_rejected(self):
        self.invalid(lambda d: d['events'][0]['bouts'][0].update(red_id='demo-fighter-missing'))

    def test_self_bout_rejected(self):
        self.invalid(lambda d: d['events'][0]['bouts'][0].update(blue_id=d['events'][0]['bouts'][0]['red_id']))

    def test_duplicate_and_noncontiguous_order_rejected(self):
        for value in (1, 99):
            with self.subTest(value=value):
                self.setUp()
                self.invalid(lambda d: d['events'][0]['bouts'][1].update(order=value))

    def test_cancelled_result_cannot_claim_winner(self):
        self.invalid(lambda d: d['events'][1]['bouts'][2].update(result='red_win'))

    def test_unknown_result_cannot_claim_method(self):
        self.invalid(lambda d: d['events'][2]['bouts'][2].update(method='KO'))

    def test_invalid_date_rejected(self):
        self.invalid(lambda d: d['events'][0].update(date='2025-02-30'))

    def test_missing_or_unknown_provenance_rejected(self):
        for value in ([], ['demo-source-missing']):
            with self.subTest(value=value):
                self.setUp()
                self.invalid(lambda d: d['fighters'][0].update(provenance=value))

    def test_real_source_and_missing_disclosure_rejected(self):
        self.invalid(lambda d: d['sources'][0].update(kind='official-site'))
        self.setUp()
        self.invalid(lambda d: d.update(notice='Event archive'))

    def test_future_schema_and_undeclared_fields_rejected(self):
        self.invalid(lambda d: d.update(schema_version=2))
        self.setUp()
        self.invalid(lambda d: d['fighters'][0].update(career_wins=100))

    def test_coverage_explicit_and_tournament_stage_valid(self):
        self.invalid(lambda d: d['events'][0].update(coverage='entire-history'))
        self.setUp()
        self.invalid(lambda d: d['events'][0]['bouts'][0]['tournament'].update(stage='winner'))

    def test_tournament_ids_do_not_collide_with_entities(self):
        self.invalid(lambda d: d['events'][0]['bouts'][0]['tournament'].update(id=d['fighters'][0]['id']))

    def test_corrections_must_match_current_value(self):
        self.invalid(lambda d: d['corrections'][0].update(new_value='Wrong'))

    def test_corrections_cannot_advance_past_dataset(self):
        self.invalid(lambda d: d['corrections'][0].update(to_revision=3))

    def test_corrections_cannot_rewrite_identity(self):
        self.invalid(lambda d: d['corrections'][0].update(field='id'))

    def test_broken_correction_chain_rejected(self):
        extra = copy.deepcopy(self.data['corrections'][0])
        extra.update(id='demo-correction-002', from_revision=2, to_revision=3, previous_value='Unrelated')
        self.data.update(revision=3)
        self.data['corrections'].append(extra)
        with self.assertRaises(ValueError):
            validate(self.data)


if __name__ == '__main__':
    unittest.main()
