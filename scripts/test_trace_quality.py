"""Synthetic trace-quality regressions; no device or trace download required."""
import unittest
import json
from pathlib import Path
import tempfile
from unittest.mock import patch

from trace_quality import assess, main


class TraceQualityTest(unittest.TestCase):
    def setUp(self):
        self.tracks = dict.fromkeys(
            ('app_slices', 'app_sched', 'app_actual_frames', 'app_expected_frames'), 1)

    def stat(self, name, value=1, severity='info'):
        return dict(name=name, idx='0', severity=severity, source='trace', value=str(value))

    def test_setup_errors_are_rejected_even_when_only_notices(self):
        result = assess([self.stat('ftrace_setup_errors', 35, 'notice')], self.tracks)
        self.assertFalse(result['attributionPrerequisitesPresent'])

    def test_data_loss_and_buffer_overwrite_are_rejected(self):
        for stat in (self.stat('unknown_future_loss', severity='data_loss'),
                     self.stat('traced_buf_bytes_overwritten'), self.stat('ftrace_cpu_overrun_end')):
            with self.subTest(stat=stat):
                self.assertFalse(assess([stat], self.tracks)['attributionPrerequisitesPresent'])

    def test_missing_tracks_are_not_success(self):
        for name in self.tracks:
            with self.subTest(track=name):
                tracks = self.tracks | {name: 0}
                self.assertFalse(assess([], tracks)['attributionPrerequisitesPresent'])

    def test_informational_stats_are_preserved_without_certifying_acceptance(self):
        stats = [self.stat('traced_buf_bytes_written', 1024), self.stat('ftrace_setup_errors', 0)]
        result = assess(stats, self.tracks)
        self.assertTrue(result['attributionPrerequisitesPresent'])
        self.assertEqual(stats, result['nonzeroStats'])
        self.assertIn('Not assessed', result['acceptance'])

    def test_failed_query_writes_failed_report_and_preserves_existing_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            trace = root / 'sample.trace'
            processor = root / 'processor'
            output = root / 'quality.json'
            trace.write_bytes(b'test trace')
            processor.write_bytes(b'\x7fELFtest processor')
            arguments = [str(trace), '--processor', str(processor), '--output', str(output)]
            with patch('trace_quality.query', side_effect=ValueError('missing table')):
                self.assertEqual(2, main(arguments))
            report = json.loads(output.read_text())
            self.assertFalse(report['attributionPrerequisitesPresent'])
            self.assertEqual(64, len(report['traceSha256']))
            before = output.read_bytes()
            with self.assertRaises(SystemExit):
                main(arguments)
            self.assertEqual(before, output.read_bytes())

    def test_download_wrapper_is_refused_before_execution(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            trace = root / 'sample.trace'
            processor = root / 'wrapper'
            trace.write_bytes(b'test trace')
            processor.write_text('#!/usr/bin/env python3\n')
            with patch('trace_quality.query') as query:
                with self.assertRaises(SystemExit):
                    main([str(trace), '--processor', str(processor),
                          '--output', str(root / 'quality.json')])
                query.assert_not_called()


if __name__ == '__main__':
    unittest.main()
