-- Supply to the official Perfetto trace_processor query -f option.
-- Inclusive slice durations overlap; do not add them as independent costs.
SELECT t.name AS thread, s.name AS section, COUNT(*) AS calls,
 ROUND(SUM(s.dur)/1e6, 2) AS inclusive_ms,
 ROUND(MAX(s.dur)/1e6, 2) AS max_ms
FROM slice s JOIN thread_track tt ON s.track_id=tt.id
JOIN thread t USING(utid) JOIN process p USING(upid)
WHERE p.name='fr.deuspheara.oree' AND s.dur>0
GROUP BY t.name,s.name ORDER BY inclusive_ms DESC LIMIT 40;

SELECT t.name AS thread, ROUND(SUM(dur)/1e6,2) AS cpu_ms
FROM sched JOIN thread t USING(utid) JOIN process p USING(upid)
WHERE p.name='fr.deuspheara.oree' GROUP BY t.name ORDER BY cpu_ms DESC;
