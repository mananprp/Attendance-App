import React, { useState } from 'react';
import { api } from '../api';
import { AdminSite } from '../types';
import {
  Building2,
  Plus,
  ExternalLink,
  RefreshCw,
  X,
  Navigation,
  Edit2,
  PowerOff,
  Power,
  AlertTriangle,
  CheckCircle,
} from 'lucide-react';

interface SitesViewProps {
  sites: AdminSite[];
  onRefreshSites: () => Promise<void>;
}

function generateSiteCodePreview(name: string): string {
  const slug = name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return 'SITE_' + (slug.length > 0 ? slug.slice(0, 45) : 'OFFICE');
}

export const SitesView: React.FC<SitesViewProps> = ({ sites, onRefreshSites }) => {
  const [loading, setLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Create Site Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [newLatitude, setNewLatitude] = useState('');
  const [newLongitude, setNewLongitude] = useState('');
  const [newRadiusM, setNewRadiusM] = useState('100');
  const [creating, setCreating] = useState(false);
  const [gettingLocation, setGettingLocation] = useState(false);

  // Edit Site Modal State
  const [editingSite, setEditingSite] = useState<AdminSite | null>(null);
  const [editName, setEditName] = useState('');
  const [editLatitude, setEditLatitude] = useState('');
  const [editLongitude, setEditLongitude] = useState('');
  const [editRadiusM, setEditRadiusM] = useState('100');
  const [updating, setUpdating] = useState(false);
  const [gettingEditLocation, setGettingEditLocation] = useState(false);

  // Deactivate Conflict Alert State
  const [deactivateError, setDeactivateError] = useState<string | null>(null);
  const [togglingSiteId, setTogglingSiteId] = useState<string | null>(null);

  const handleRefresh = async () => {
    setLoading(true);
    setActionError(null);
    await onRefreshSites();
    setLoading(false);
  };

  const handleUseCurrentLocationForCreate = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }
    setGettingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setNewLatitude(pos.coords.latitude.toFixed(6));
        setNewLongitude(pos.coords.longitude.toFixed(6));
        setGettingLocation(false);
      },
      (err) => {
        alert(`Failed to get location: ${err.message}`);
        setGettingLocation(false);
      },
      { enableHighAccuracy: true },
    );
  };

  const handleUseCurrentLocationForEdit = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }
    setGettingEditLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setEditLatitude(pos.coords.latitude.toFixed(6));
        setEditLongitude(pos.coords.longitude.toFixed(6));
        setGettingEditLocation(false);
      },
      (err) => {
        alert(`Failed to get location: ${err.message}`);
        setGettingEditLocation(false);
      },
      { enableHighAccuracy: true },
    );
  };

  const handleCreateSite = async (e: React.FormEvent) => {
    e.preventDefault();
    const lat = parseFloat(newLatitude);
    const lng = parseFloat(newLongitude);
    const rad = parseInt(newRadiusM, 10) || 100;

    if (isNaN(lat) || isNaN(lng)) {
      alert('Please enter valid numeric latitude and longitude coordinates.');
      return;
    }

    try {
      setCreating(true);
      setActionError(null);
      await api.createSite({
        name: newName.trim(),
        latitude: lat,
        longitude: lng,
        radius_m: rad,
        timezone: 'Asia/Kolkata',
      });
      setShowCreateModal(false);
      setNewName('');
      setNewLatitude('');
      setNewLongitude('');
      setNewRadiusM('100');
      setActionSuccess('Office site created successfully!');
      setTimeout(() => setActionSuccess(null), 3000);
      await onRefreshSites();
    } catch (err: any) {
      setActionError(err.message || 'Failed to create site');
    } finally {
      setCreating(false);
    }
  };

  const handleOpenEdit = (site: AdminSite) => {
    setEditingSite(site);
    setEditName(site.name);
    setEditLatitude(site.latitude.toString());
    setEditLongitude(site.longitude.toString());
    setEditRadiusM(site.radius_m.toString());
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSite) return;

    const lat = parseFloat(editLatitude);
    const lng = parseFloat(editLongitude);
    const rad = parseInt(editRadiusM, 10) || 100;

    if (isNaN(lat) || isNaN(lng)) {
      alert('Please enter valid numeric latitude and longitude coordinates.');
      return;
    }

    try {
      setUpdating(true);
      setActionError(null);
      await api.updateSite(editingSite.office_id, {
        name: editName.trim(),
        latitude: lat,
        longitude: lng,
        radius_m: rad,
      });
      setEditingSite(null);
      setActionSuccess(`Site '${editName.trim()}' updated successfully!`);
      setTimeout(() => setActionSuccess(null), 3000);
      await onRefreshSites();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update site');
    } finally {
      setUpdating(false);
    }
  };

  const handleToggleActive = async (site: AdminSite) => {
    setDeactivateError(null);
    const isCurrentlyActive = site.is_active !== false;

    if (isCurrentlyActive) {
      const confirmDeactivate = window.confirm(
        `Are you sure you want to deactivate '${site.name}' (${site.office_id})?\nEmployees will not be able to punch at this location while deactivated.`,
      );
      if (!confirmDeactivate) return;

      try {
        setTogglingSiteId(site.office_id);
        await api.deactivateSite(site.office_id);
        setActionSuccess(`Site '${site.name}' deactivated successfully.`);
        setTimeout(() => setActionSuccess(null), 3000);
        await onRefreshSites();
      } catch (err: any) {
        setDeactivateError(err.message || 'Failed to deactivate site.');
      } finally {
        setTogglingSiteId(null);
      }
    } else {
      try {
        setTogglingSiteId(site.office_id);
        await api.activateSite(site.office_id);
        setActionSuccess(`Site '${site.name}' re-activated successfully.`);
        setTimeout(() => setActionSuccess(null), 3000);
        await onRefreshSites();
      } catch (err: any) {
        setActionError(err.message || 'Failed to activate site.');
      } finally {
        setTogglingSiteId(null);
      }
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Office Site Locations</h2>
            <p className="card-subtitle">
              Configured geofenced locations with customizable coordinates and boundaries
            </p>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={() => {
                setActionError(null);
                setShowCreateModal(true);
              }}
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

        {actionSuccess && (
          <div style={{ padding: '12px 24px', background: 'var(--success-bg, #ecfdf5)', color: 'var(--success, #059669)', fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: 8 }}>
            <CheckCircle size={16} /> {actionSuccess}
          </div>
        )}

        {actionError && (
          <div style={{ padding: '12px 24px', background: 'var(--error-bg)', color: 'var(--error)', fontSize: '0.88rem', display: 'flex', alignItems: 'center', gap: 8 }}>
            <AlertTriangle size={16} /> {actionError}
          </div>
        )}

        {deactivateError && (
          <div style={{ padding: '14px 24px', background: '#fff1f2', borderLeft: '4px solid #e11d48', color: '#9f1239', fontSize: '0.88rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <AlertTriangle size={18} color="#e11d48" />
              <span>{deactivateError}</span>
            </div>
            <button
              onClick={() => setDeactivateError(null)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9f1239', fontWeight: 600 }}
            >
              Dismiss
            </button>
          </div>
        )}

        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Site Code</th>
                <th>Site Name</th>
                <th>GPS Coordinates</th>
                <th>Geofence Radius</th>
                <th>Status</th>
                <th>Timezone</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sites.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-secondary)' }}>
                    No office sites configured. Click "Create New Site" to add one.
                  </td>
                </tr>
              ) : (
                sites.map((site) => {
                  const isActive = site.is_active !== false;
                  return (
                    <tr key={site.office_id} style={{ opacity: isActive ? 1 : 0.65 }}>
                      <td>
                        <span style={{ fontWeight: 700, fontFamily: 'monospace', color: 'var(--primary)' }}>
                          {site.office_id}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <Building2 size={16} color="var(--primary)" />
                          <span style={{ fontWeight: 600 }}>{site.name}</span>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span style={{ fontSize: '0.85rem', fontFamily: 'monospace' }}>
                            {site.latitude.toFixed(5)}, {site.longitude.toFixed(5)}
                          </span>
                          <a
                            href={`https://www.google.com/maps/search/?api=1&query=${site.latitude},${site.longitude}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            title="Open in Google Maps"
                            style={{ color: 'var(--primary)', display: 'inline-flex', alignItems: 'center' }}
                          >
                            <ExternalLink size={14} />
                          </a>
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-office">{site.radius_m}m</span>
                      </td>
                      <td>
                        {isActive ? (
                          <span className="badge badge-office" style={{ background: '#ecfdf5', color: '#059669', border: '1px solid #a7f3d0' }}>
                            Active
                          </span>
                        ) : (
                          <span className="badge badge-employee" style={{ background: '#f3f4f6', color: '#6b7280', border: '1px solid #e5e7eb' }}>
                            Deactivated
                          </span>
                        )}
                      </td>
                      <td>
                        <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          {site.timezone || 'Asia/Kolkata'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: 8, justifyContent: 'flex-end' }}>
                          <button
                            onClick={() => handleOpenEdit(site)}
                            className="btn btn-secondary btn-sm"
                            title="Edit site name, GPS coordinates, and radius"
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          >
                            <Edit2 size={13} /> Edit
                          </button>
                          {isActive ? (
                            <button
                              onClick={() => handleToggleActive(site)}
                              disabled={togglingSiteId === site.office_id}
                              className="btn btn-secondary btn-sm"
                              title="Deactivate site (blocked if employees are assigned)"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color: '#e11d48', borderColor: '#fecdd3' }}
                            >
                              <PowerOff size={13} /> Deactivate
                            </button>
                          ) : (
                            <button
                              onClick={() => handleToggleActive(site)}
                              disabled={togglingSiteId === site.office_id}
                              className="btn btn-secondary btn-sm"
                              title="Re-activate site for employee punches"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color: '#059669', borderColor: '#a7f3d0' }}
                            >
                              <Power size={13} /> Activate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
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
                  <label className="filter-label">Site Name</label>
                  <input
                    type="text"
                    className="input"
                    placeholder="e.g. Mumbai Regional Office"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Site Code (Auto-generated)</label>
                  <input
                    type="text"
                    className="input"
                    value={newName.trim() ? generateSiteCodePreview(newName) : 'SITE_...'}
                    disabled
                    style={{ background: '#f8fafc', color: 'var(--text-secondary)', fontFamily: 'monospace' }}
                  />
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                    Site code is permanent and generated automatically from the site name.
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                  <button
                    type="button"
                    onClick={handleUseCurrentLocationForCreate}
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
                      placeholder="e.g. 19.076000"
                      value={newLatitude}
                      onChange={(e) => setNewLatitude(e.target.value)}
                      required
                    />
                  </div>
                  <div className="filter-group">
                    <label className="filter-label">Longitude</label>
                    <input
                      type="number"
                      step="any"
                      className="input"
                      placeholder="e.g. 72.877700"
                      value={newLongitude}
                      onChange={(e) => setNewLongitude(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Geofence Radius (meters)</label>
                  <input
                    type="number"
                    className="input"
                    value={newRadiusM}
                    onChange={(e) => setNewRadiusM(e.target.value)}
                    required
                    min={10}
                    max={10000}
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
                  disabled={creating || !newName.trim()}
                  className="btn btn-primary"
                >
                  {creating ? 'Creating...' : 'Create Office Site'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Site Modal */}
      {editingSite && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Edit Office Site</h3>
                <span style={{ fontSize: '0.78rem', fontFamily: 'monospace', color: 'var(--text-secondary)' }}>
                  Code: {editingSite.office_id} (Locked)
                </span>
              </div>
              <button
                onClick={() => setEditingSite(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveEdit}>
              <div className="modal-body">
                <div className="filter-group">
                  <label className="filter-label">Site Name</label>
                  <input
                    type="text"
                    className="input"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
                  <button
                    type="button"
                    onClick={handleUseCurrentLocationForEdit}
                    disabled={gettingEditLocation}
                    className="btn btn-secondary btn-sm"
                  >
                    <Navigation size={14} /> {gettingEditLocation ? 'Acquiring GPS...' : 'Use Current Coordinates'}
                  </button>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="filter-group">
                    <label className="filter-label">Latitude</label>
                    <input
                      type="number"
                      step="any"
                      className="input"
                      value={editLatitude}
                      onChange={(e) => setEditLatitude(e.target.value)}
                      required
                    />
                  </div>
                  <div className="filter-group">
                    <label className="filter-label">Longitude</label>
                    <input
                      type="number"
                      step="any"
                      className="input"
                      value={editLongitude}
                      onChange={(e) => setEditLongitude(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Geofence Radius (meters)</label>
                  <input
                    type="number"
                    className="input"
                    value={editRadiusM}
                    onChange={(e) => setEditRadiusM(e.target.value)}
                    required
                    min={10}
                    max={10000}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setEditingSite(null)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updating || !editName.trim()}
                  className="btn btn-primary"
                >
                  {updating ? 'Saving Changes...' : 'Save Site Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
