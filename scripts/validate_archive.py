#!/usr/bin/env python3
"""Validate the bundled fictional archive. Standard library only; no downloads or writes."""
import argparse
from datetime import date
import json
from pathlib import Path
import re

DEFAULT = Path(__file__).resolve().parents[1] / 'app/src/main/assets/archive-demo-v1.json'
ID = re.compile(r'demo-[a-z0-9]+(?:-[a-z0-9]+)*\Z')


def require(ok, message):
    if not ok:
        raise ValueError(message)


def obj(value, fields):
    require(type(value) is dict and set(value) == set(fields.split()), 'Unexpected or missing fields')


def text(value):
    require(type(value) is str and bool(value.strip()) and value == value.strip(), 'Nonempty trimmed text required')


def integer(value):
    require(type(value) is int and value > 0, 'Positive integer required')


def array(value, nonempty=False):
    require(type(value) is list and (not nonempty or bool(value)), 'Array required')


def strings(value):
    array(value)
    for item in value:
        text(item)
    require(len(value) == len(set(value)), 'Duplicate string')


def validate(data):
    obj(data, 'schema_version id revision kind title notice coverage sources fighters events corrections')
    require(type(data['schema_version']) is int and data['schema_version'] == 1, 'Unsupported schema version')
    require(data['kind'] == 'synthetic-demo', 'Only synthetic demonstration data is permitted')
    require('fictional' in data['notice'].lower() and 'demo' in data['notice'].lower(), 'Demo disclosure required')
    for key in ('title', 'notice', 'coverage'):
        text(data[key])
    integer(data['revision'])
    seen = set()
    targets = {}

    def identify(item):
        require(type(item['id']) is str and bool(ID.fullmatch(item['id'])), 'Stable demo ID required')
        require(item['id'] not in seen, 'Duplicate ID')
        seen.add(item['id'])
        targets[item['id']] = item

    identify(data)
    for key in ('sources', 'fighters', 'events', 'corrections'):
        array(data[key], nonempty=key != 'corrections')
    for source in data['sources']:
        obj(source, 'id kind label')
        identify(source)
        require(source['kind'] == 'synthetic', 'Factual sources are not approved')
        text(source['label'])
    sources = {s['id'] for s in data['sources']}

    def provenance(item):
        strings(item['provenance'])
        require(bool(item['provenance']) and set(item['provenance']) <= sources, 'Unknown/missing provenance')

    for fighter in data['fighters']:
        obj(fighter, 'id name aliases provenance')
        identify(fighter)
        text(fighter['name'])
        strings(fighter['aliases'])
        provenance(fighter)
    fighters = {f['id'] for f in data['fighters']}
    tournament_events = {}
    bout_count = 0
    for event in data['events']:
        obj(event, 'id title date venue location aliases coverage provenance bouts')
        identify(event)
        for key in ('title', 'venue', 'location'):
            text(event[key])
        require(type(event['date']) is str and re.fullmatch(r'\d{4}-\d{2}-\d{2}', event['date']), 'ISO date required')
        date.fromisoformat(event['date'])
        strings(event['aliases'])
        require(event['coverage'] in ('complete-demo-card', 'partial-demo-card'), 'Unknown event coverage')
        provenance(event)
        array(event['bouts'], nonempty=True)
        orders = set()
        for bout in event['bouts']:
            obj(bout, 'id order red_id blue_id status result method round tournament provenance')
            identify(bout)
            integer(bout['order'])
            require(bout['order'] not in orders, 'Duplicate bout order')
            orders.add(bout['order'])
            require(bout['red_id'] in fighters and bout['blue_id'] in fighters and bout['red_id'] != bout['blue_id'], 'Invalid bout participants')
            require(bout['status'] in ('completed', 'cancelled', 'unknown'), 'Invalid bout status')
            allowed = {'completed': ('red_win', 'blue_win', 'draw', 'no_contest', 'unknown'), 'cancelled': ('not_applicable',), 'unknown': ('unknown',)}
            require(bout['result'] in allowed[bout['status']], 'Result incompatible with status')
            if bout['method'] is not None:
                text(bout['method'])
            if bout['round'] is not None:
                integer(bout['round'])
            if bout['status'] != 'completed':
                require(bout['method'] is None and bout['round'] is None, 'Unresolved/cancelled result must not claim method or round')
            provenance(bout)
            tournament = bout['tournament']
            if tournament is not None:
                obj(tournament, 'id stage')
                require(type(tournament['id']) is str and bool(ID.fullmatch(tournament['id'])), 'Stable tournament ID required')
                require(tournament['stage'] in ('quarterfinal', 'semifinal', 'final', 'reserve', 'unknown'), 'Invalid tournament stage')
                require(tournament['id'] not in seen, 'Tournament ID collides with entity')
                require(tournament_events.setdefault(tournament['id'], event['id']) == event['id'], 'Tournament spans different events')
            bout_count += 1
        require(orders == set(range(1, len(orders)+1)), 'Bout order must be contiguous from one')
    require(not (set(tournament_events) & seen), 'Tournament ID collides with entity')
    previous = {}
    for correction in data['corrections']:
        obj(correction, 'id from_revision to_revision target_id field previous_value new_value reason provenance')
        identify(correction)
        integer(correction['from_revision'])
        integer(correction['to_revision'])
        require(correction['from_revision'] < correction['to_revision'] <= data['revision'], 'Invalid correction revision')
        target = targets.get(correction['target_id'])
        field = correction['field']
        require(field in ('name', 'title', 'method', 'result', 'coverage') and target is not None and field in target and target is not correction, 'Invalid correction target/field')
        for key in ('previous_value', 'new_value', 'reason'):
            text(correction[key])
        provenance(correction)
        pair = (correction['target_id'], field)
        if pair in previous:
            prior = previous[pair]
            require(prior['to_revision'] <= correction['from_revision'] and prior['new_value'] == correction['previous_value'], 'Broken correction history')
        previous[pair] = correction
    for (target_id, field), correction in previous.items():
        require(targets[target_id][field] == correction['new_value'], 'Correction does not match current value')
    return {'kind': data['kind'], 'schema_version': 1, 'revision': data['revision'], 'events': len(data['events']), 'fighters': len(fighters), 'bouts': bout_count, 'corrections': len(data['corrections'])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--file', type=Path, default=DEFAULT)
    args = parser.parse_args()
    try:
        result = validate(json.loads(args.file.read_text(encoding='utf-8')))
    except (ValueError, OSError, TypeError, KeyError) as error:
        print(json.dumps({'status': 'INVALID', 'reason': str(error) if isinstance(error, ValueError) else 'Malformed or unreadable archive'}))
        return 1
    print(json.dumps(dict(status='PASS', **result), indent=2))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
