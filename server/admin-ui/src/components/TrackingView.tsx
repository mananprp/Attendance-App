import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import { TrackingSessionSummary, TrackingSessionDetails } from '../types';
import { Navigation, MapPin, Calendar, Clock, AlertTriangle, X } from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default icon issues in bundlers
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

export const TrackingView: React.FC = () => {
  const [sessions, setSessions] = useState<TrackingSessionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedSession, setSelectedSession] = useState<TrackingSessionDetails | null>(null);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [userIdFilter, setUserIdFilter] = useState('');
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);

  const loadSessions = async () => {
    setLoading(true);
    try {
      const data = await api.getTrackingSessions(userIdFilter || undefined);
      setSessions(data);
    } catch (err) {
      console.error('Failed to load tracking sessions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSessions();
  }, [userIdFilter]);

  const handleOpenMap = async (sessionId: string) => {
    setLoadingDetails(true);
    try {
      const details = await api.getTrackingSessionDetails(sessionId);
      setSelectedSession(details);
    } catch (err) {
      console.error('Failed to load session details', err);
    } finally {
      setLoadingDetails(false);
    }
  };

  const handleCloseMap = () => {
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }
    setSelectedSession(null);
  };

  // Render Leaflet Map when selectedSession is set
  useEffect(() => {
    if (!selectedSession || !mapContainerRef.current) return;

    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }

    const points = selectedSession.points || [];
    const stops = selectedSession.stops || [];

    const defaultCenter: [number, number] =
      points.length > 0 ? [points[0].latitude, points[0].longitude] : [28.6139, 77.209];

    const map = L.map(mapContainerRef.current).setView(defaultCenter, 14);
    mapInstanceRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map);

    if (points.length > 0) {
      const latLngs: [number, number][] = points.map((p) => [p.latitude, p.longitude]);
      const polyline = L.polyline(latLngs, {
        color: '#2563EB',
        weight: 4,
        opacity: 0.85,
      }).addTo(map);

      // Start Marker (Green)
      const startPoint = points[0];
      const startIcon = L.divIcon({
        className: 'custom-map-pin start',
        html: `<div style="background:#10B981;width:24px;height:24px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:bold;">S</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });
      L.marker([startPoint.latitude, startPoint.longitude], { icon: startIcon })
        .addTo(map)
        .bindPopup(`<b>Trip Start</b><br>${new Date(startPoint.recordedAtUtc).toLocaleTimeString()}`);

      // End Marker (Red)
      const endPoint = points[points.length - 1];
      const endIcon = L.divIcon({
        className: 'custom-map-pin end',
        html: `<div style="background:#EF4444;width:24px;height:24px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:bold;">E</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });
      L.marker([endPoint.latitude, endPoint.longitude], { icon: endIcon })
        .addTo(map)
        .bindPopup(`<b>Trip Finish</b><br>${new Date(endPoint.recordedAtUtc).toLocaleTimeString()}`);

      // Stops Markers (Orange)
      stops.forEach((stop, idx) => {
        const stopIcon = L.divIcon({
          className: 'custom-map-pin stop',
          html: `<div style="background:#F59E0B;width:26px;height:26px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:bold;">${idx + 1}</div>`,
          iconSize: [26, 26],
          iconAnchor: [13, 13],
        });
        L.marker([stop.latitude, stop.longitude], { icon: stopIcon })
          .addTo(map)
          .bindPopup(
            `<b>Stop ${idx + 1}: ${stop.stopName || 'Site Inspection'}</b><br>Arrived: ${new Date(stop.arrivedAtUtc).toLocaleTimeString()}`,
          );
      });

      map.fitBounds(polyline.getBounds(), { padding: [40, 40] });
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [selectedSession]);

  const formatDistance = (meters: number) => {
    if (!meters || meters < 1000) return `${Math.round(meters || 0)} m`;
    return `${(meters / 1000).toFixed(2)} km`;
  };

  const formatDuration = (startStr: string, endStr?: string) => {
    if (!endStr) return 'Active now';
    const start = new Date(startStr).getTime();
    const end = new Date(endStr).getTime();
    const mins = Math.max(0, Math.round((end - start) / 60000));
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    if (hrs > 0) return `${hrs}h ${remMins}m`;
    return `${mins}m`;
  };

  return (
    <div className="view-container">
      <div className="view-header">
        <div>
          <h2>Field & Supervisor Tracking</h2>
          <p>Review real-time and completed site visit routes, distances, and inspection stops.</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <input
            type="text"
            placeholder="Filter by Employee ID..."
            value={userIdFilter}
            onChange={(e) => setUserIdFilter(e.target.value)}
            className="input"
            style={{ width: 220 }}
          />
          <button className="btn btn-secondary" onClick={loadSessions}>
            Refresh
          </button>
        </div>
      </div>

      <div className="table-container">
        <table className="table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Date & Start Time</th>
              <th>End Time</th>
              <th>Duration</th>
              <th>Distance</th>
              <th>Stops</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '32px' }}>
                  Loading tracking sessions...
                </td>
              </tr>
            ) : sessions.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                  No tracking sessions found.
                </td>
              </tr>
            ) : (
              sessions.map((session) => (
                <tr key={session.id}>
                  <td>
                    <div style={{ fontWeight: 600 }}>{session.user?.displayName || session.userId}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>ID: {session.userId}</div>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <Calendar size={14} color="var(--text-muted)" />
                      <span>{new Date(session.startTimeUtc).toLocaleDateString()}</span>
                    </div>
                    <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                      {new Date(session.startTimeUtc).toLocaleTimeString()}
                    </div>
                  </td>
                  <td>
                    {session.endTimeUtc ? (
                      <div>
                        <div>{new Date(session.endTimeUtc).toLocaleDateString()}</div>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                          {new Date(session.endTimeUtc).toLocaleTimeString()}
                        </div>
                      </div>
                    ) : (
                      <span style={{ color: 'var(--success)', fontWeight: 600 }}>In Progress</span>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <Clock size={14} color="var(--text-muted)" />
                      <span>{formatDuration(session.startTimeUtc, session.endTimeUtc)}</span>
                    </div>
                  </td>
                  <td style={{ fontWeight: 600 }}>{formatDistance(session.totalDistanceM)}</td>
                  <td>
                    <span className="badge" style={{ background: 'var(--surface-hover)' }}>
                      {session.stopsCount} stops
                    </span>
                  </td>
                  <td>
                    <span
                      className="badge"
                      style={{
                        background:
                          session.status === 'COMPLETED'
                            ? 'rgba(16, 185, 129, 0.15)'
                            : session.status === 'ACTIVE'
                            ? 'rgba(59, 130, 246, 0.15)'
                            : 'rgba(239, 68, 68, 0.15)',
                        color:
                          session.status === 'COMPLETED'
                            ? '#10B981'
                            : session.status === 'ACTIVE'
                            ? '#3B82F6'
                            : '#EF4444',
                      }}
                    >
                      {session.status}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-sm btn-primary"
                      onClick={() => handleOpenMap(session.id)}
                      disabled={loadingDetails}
                      style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      <Navigation size={14} />
                      <span>View Route</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Map Modal */}
      {selectedSession && (
        <div className="modal-backdrop">
          <div className="modal-dialog" style={{ maxWidth: 900, width: '95%', maxHeight: '90vh' }}>
            <div className="modal-header">
              <div>
                <h3 style={{ margin: 0 }}>
                  Route Map: {selectedSession.user?.displayName || selectedSession.userId}
                </h3>
                <div style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4 }}>
                  {new Date(selectedSession.startTimeUtc).toLocaleString()} • Distance:{' '}
                  {formatDistance(selectedSession.totalDistanceM)} • Stops: {selectedSession.stopsCount}
                </div>
              </div>
              <button className="btn-close" onClick={handleCloseMap}>
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ padding: 0 }}>
              <div
                ref={mapContainerRef}
                style={{
                  width: '100%',
                  height: 480,
                  background: '#f3f4f6',
                }}
              />
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: 16, fontSize: 13, alignItems: 'center' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#10B981' }}></span>
                  Start
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#F59E0B' }}></span>
                  Site Stops
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#EF4444' }}></span>
                  Finish
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span style={{ width: 16, height: 3, background: '#2563EB' }}></span>
                  Tracked Route ({selectedSession.points?.length || 0} coordinates)
                </span>
              </div>
              <button className="btn btn-secondary" onClick={handleCloseMap}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
