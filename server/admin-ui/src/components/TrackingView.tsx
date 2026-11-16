import React, { useState, useEffect, useRef } from 'react';
import { api } from '../api';
import { TrackingSessionSummary, TrackingSessionDetails } from '../types';
import { Navigation, MapPin, Calendar, Clock, AlertTriangle, X, RefreshCw, CheckCircle } from 'lucide-react';
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
        html: `<div style="background:#10B981;width:24px;height:24px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:bold;box-shadow:0 2px 4px rgba(0,0,0,0.2);">S</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });
      L.marker([startPoint.latitude, startPoint.longitude], { icon: startIcon })
        .bindPopup(
          `<strong>Trip Start</strong><br/>Time: ${new Date(startPoint.recordedAtUtc).toLocaleTimeString()}<br/>Accuracy: ${Math.round(startPoint.accuracyM || 0)}m`
        )
        .addTo(map);

      // End Marker (Red if finished)
      if (selectedSession.endTimeUtc && points.length > 1) {
        const endPoint = points[points.length - 1];
        const endIcon = L.divIcon({
          className: 'custom-map-pin end',
          html: `<div style="background:#EF4444;width:24px;height:24px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:11px;font-weight:bold;box-shadow:0 2px 4px rgba(0,0,0,0.2);">F</div>`,
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        });
        L.marker([endPoint.latitude, endPoint.longitude], { icon: endIcon })
          .bindPopup(
            `<strong>Trip Finish</strong><br/>Time: ${new Date(endPoint.recordedAtUtc).toLocaleTimeString()}<br/>Total Distance: ${formatDistance(selectedSession.totalDistanceM)}`
          )
          .addTo(map);
      }

      // Fit map bounds to polyline
      map.fitBounds(polyline.getBounds(), { padding: [40, 40] });
    }

    // Render Stops (Amber Pins)
    stops.forEach((stop, idx) => {
      const stopIcon = L.divIcon({
        className: 'custom-map-pin stop',
        html: `<div style="background:#F59E0B;width:22px;height:22px;border-radius:50%;border:2px solid #fff;display:flex;align-items:center;justify-content:center;color:#fff;font-size:10px;font-weight:bold;box-shadow:0 2px 4px rgba(0,0,0,0.2);">${idx + 1}</div>`,
        iconSize: [22, 22],
        iconAnchor: [11, 11],
      });

      const arrivalTime = new Date(stop.arrivedAtUtc).toLocaleTimeString();

      L.marker([stop.latitude, stop.longitude], { icon: stopIcon })
        .bindPopup(
          `<strong>Stop #${idx + 1}: ${stop.stopName || 'Site Stop'}</strong><br/>Arrived: ${arrivalTime}`
        )
        .addTo(map);
    });
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

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="badge badge-complete">
            <CheckCircle size={12} /> Completed
          </span>
        );
      case 'ACTIVE':
        return (
          <span className="badge badge-in-progress">
            <Clock size={12} /> In Progress
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="badge badge-missed">
            <AlertTriangle size={12} /> Cancelled
          </span>
        );
      default:
        return <span className="badge badge-employee">{status}</span>;
    }
  };

  const activeCount = sessions.filter((s) => s.status === 'ACTIVE').length;
  const completedCount = sessions.filter((s) => s.status === 'COMPLETED').length;
  const totalStops = sessions.reduce((acc, s) => acc + (s.stopsCount || 0), 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Overview Metric Stat Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}>
            <Navigation size={24} />
          </div>
          <div>
            <div className="stat-value">{sessions.length}</div>
            <div className="stat-label">Total Tracking Sessions</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(59, 130, 246, 0.1)', color: '#3B82F6' }}>
            <Clock size={24} />
          </div>
          <div>
            <div className="stat-value">{activeCount}</div>
            <div className="stat-label">Active In Progress</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#10B981' }}>
            <CheckCircle size={24} />
          </div>
          <div>
            <div className="stat-value">{completedCount}</div>
            <div className="stat-label">Completed Sessions</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#F59E0B' }}>
            <MapPin size={24} />
          </div>
          <div>
            <div className="stat-value">{totalStops}</div>
            <div className="stat-label">Inspection Stops Logged</div>
          </div>
        </div>
      </div>

      {/* Main Card with Header, Filter Bar, and Table */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Field & Supervisor Tracking</h2>
            <p className="card-subtitle">
              Review real-time and completed site visit routes, distances, and inspection stops
            </p>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={loadSessions}
              disabled={loading}
              className="btn btn-secondary"
              title="Refresh sessions list"
            >
              <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>

        {/* Filter Controls Bar */}
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Filter by Employee ID</label>
            <input
              type="text"
              placeholder="Enter Employee ID (e.g. EMP2001)..."
              value={userIdFilter}
              onChange={(e) => setUserIdFilter(e.target.value)}
              className="input"
              style={{ minWidth: 260 }}
            />
          </div>
          {userIdFilter && (
            <div className="filter-group" style={{ alignSelf: 'flex-end' }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => setUserIdFilter('')}
                title="Clear employee filter"
              >
                Clear
              </button>
            </div>
          )}
        </div>

        {/* Standard Data Table */}
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Date & Start Time</th>
                <th>End Time</th>
                <th>Duration</th>
                <th>Distance</th>
                <th>Stops</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-secondary)' }}>
                    <RefreshCw size={20} className="spin" style={{ margin: '0 auto 8px auto', display: 'block' }} />
                    Loading tracking sessions...
                  </td>
                </tr>
              ) : sessions.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--text-secondary)' }}>
                    No tracking sessions found.
                  </td>
                </tr>
              ) : (
                sessions.map((session) => (
                  <tr key={session.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{session.user?.displayName || session.userId}</div>
                      <span style={{ fontSize: '0.75rem', fontFamily: 'monospace', color: 'var(--text-secondary)' }}>
                        {session.userId}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 500 }}>
                        <Calendar size={14} color="var(--primary)" />
                        <span>{new Date(session.startTimeUtc).toLocaleDateString()}</span>
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginLeft: 20 }}>
                        {new Date(session.startTimeUtc).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td>
                      {session.endTimeUtc ? (
                        <div>
                          <div style={{ fontWeight: 500 }}>{new Date(session.endTimeUtc).toLocaleDateString()}</div>
                          <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            {new Date(session.endTimeUtc).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </div>
                        </div>
                      ) : (
                        <span className="badge badge-in-progress" style={{ fontSize: '0.75rem' }}>Active Now</span>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Clock size={14} color="var(--text-secondary)" />
                        <span>{formatDuration(session.startTimeUtc, session.endTimeUtc)}</span>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600 }}>{formatDistance(session.totalDistanceM)}</span>
                    </td>
                    <td>
                      <span className="badge badge-employee" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <MapPin size={11} color="var(--primary)" />
                        {session.stopsCount} stops
                      </span>
                    </td>
                    <td>
                      {renderStatusBadge(session.status)}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleOpenMap(session.id)}
                        disabled={loadingDetails}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                        title="View GPS route and inspection stops on map"
                      >
                        <Navigation size={13} color="var(--primary)" />
                        <span>View Route</span>
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Map Modal */}
      {selectedSession && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: 900, width: '95%' }}>
            <div className="modal-header">
              <div>
                <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 700 }}>
                  Route Map: {selectedSession.user?.displayName || selectedSession.userId}
                </h3>
                <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                  {new Date(selectedSession.startTimeUtc).toLocaleString()} • Distance:{' '}
                  {formatDistance(selectedSession.totalDistanceM)} • Stops: {selectedSession.stopsCount}
                </div>
              </div>
              <button
                onClick={handleCloseMap}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
                title="Close map"
              >
                <X size={20} />
              </button>
            </div>

            <div className="modal-body" style={{ padding: 0 }}>
              <div
                ref={mapContainerRef}
                style={{
                  width: '100%',
                  height: 480,
                  background: '#f8fafc',
                }}
              />
            </div>

            <div className="modal-footer" style={{ justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', gap: 16, fontSize: '0.82rem', alignItems: 'center', flexWrap: 'wrap' }}>
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
