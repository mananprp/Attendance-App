import React, { useState } from 'react';
import { api } from '../api';
import { AdminSite } from '../types';
import { Building2, Plus, ExternalLink, RefreshCw, X, Navigation } from 'lucide-react';

interface SitesViewProps {
  sites: AdminSite[];
  onRefreshSites: () => Promise<void>;
}

export const SitesView: React.FC<SitesViewProps> = ({ sites, onRefreshSites }) => {
  const [loading, setLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [officeId, setOfficeId] = useState('');
  const [name, setName] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [radiusM, setRadiusM] = useState('100');
  const [creating, setCreating] = useState(false);
  const [gettingLocation, setGettingLocation] = useState(false);

  const handleRefresh = async () => {
    setLoading(true);
    await onRefreshSites();
    setLoading(false);
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }
    setGettingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLatitude(pos.coords.latitude.toFixed(6));
        setLongitude(pos.coords.longitude.toFixed(6));
        setGettingLocation(false);
      },
      (err) => {
        alert(`Failed to get location: ${err.message}`);
        setGettingLocation(false);
      },
      { enableHighAccuracy: true },
    );
  };

  const handleCreateSite = async (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    const rad = parseInt(radiusM, 10) || 100;

    if (isNaN(lat) || isNaN(lng)) {
      alert('Please enter valid numeric latitude and longitude coordinates.');
      return;
    }

    try {
      setCreating(true);
      await api.createSite({
        office_id: officeId.trim().toUpperCase(),
        name: name.trim(),
        latitude: lat,
        longitude: lng,
        radius_m: rad,
        timezone: 'Asia/Kolkata',
      });
      setShowCreateModal(false);
      setOfficeId('');
      setName('');
      setLatitude('');
      setLongitude('');
      setRadiusM('100');
      await onRefreshSites();
    } catch (err: any) {
      alert(`Failed to create site: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Office Site Locations</h2>
            <p className="card-subtitle">
              Configured geofenced locations where employees can punch in/out
            </p>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary"
            >
              <Plus size={16} /> Create New Site
            </button>
            <button
              onClick={handleRefresh}
              disabled={loading}
              className="btn btn-secondary"
            >
              <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>

        <div style={{ padding: 24 }}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
              gap: 20,
            }}
          >
            {sites.map((site) => (
              <div
                key={site.office_id}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 12,
                  padding: 20,
                  background: '#ffffff',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 12,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div
                      style={{
                        width: 38,
                        height: 38,
                        borderRadius: 10,
                        background: 'var(--primary-light)',
                        color: 'var(--primary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <Building2 size={20} />
                    </div>
                    <div>
                      <h3 style={{ fontSize: '1rem', fontWeight: 700 }}>{site.name}</h3>
                      <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                        {site.office_id}
                      </span>
                    </div>
                  </div>
                  <span className="badge badge-office">{site.radius_m}m Geofence</span>
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <div>
                    <strong>GPS:</strong> {site.latitude.toFixed(6)}, {site.longitude.toFixed(6)}
                  </div>
                  <div>
                    <strong>Timezone:</strong> {site.timezone || 'Asia/Kolkata'}
                  </div>
                </div>

                <div style={{ paddingTop: 8, borderTop: '1px solid var(--border)' }}>
                  <a
                    href={`https://www.google.com/maps/search/?api=1&query=${site.latitude},${site.longitude}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="map-link"
                    style={{ fontSize: '0.85rem' }}
                  >
                    <ExternalLink size={14} /> Open Location in Google Maps
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Create Site Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Create New Office Site</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateSite}>
              <div className="modal-body">
                <div className="filter-group">
                  <label className="filter-label">Office ID / Code</label>
                  <input
                    type="text"
                    className="input"
                    placeholder="e.g. SITE_MUMBAI"
                    value={officeId}
                    onChange={(e) => setOfficeId(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Site Name</label>
                  <input
                    type="text"
                    className="input"
                    placeholder="e.g. Mumbai Regional Office"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    type="button"
                    onClick={handleUseCurrentLocation}
                    disabled={gettingLocation}
                    className="btn btn-secondary btn-sm"
                  >
                    <Navigation size={14} /> {gettingLocation ? 'Acquiring GPS...' : 'Use Current Coordinates'}
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="filter-group">
                    <label className="filter-label">Latitude</label>
                    <input
                      type="number"
                      step="any"
                      className="input"
                      placeholder="e.g. 19.0760"
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value)}
                      required
                    />
                  </div>
                  <div className="filter-group">
                    <label className="filter-label">Longitude</label>
                    <input
                      type="number"
                      step="any"
                      className="input"
                      placeholder="e.g. 72.8777"
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Geofence Radius (meters)</label>
                  <input
                    type="number"
                    className="input"
                    value={radiusM}
                    onChange={(e) => setRadiusM(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="btn btn-primary"
                >
                  {creating ? 'Creating...' : 'Create Office Site'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
