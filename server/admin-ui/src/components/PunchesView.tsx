import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { AdminDailyAttendance, AdminSite, AdminUser } from '../types';
import {
  Download,
  Filter,
  RefreshCw,
  MapPin,
  CheckCircle,
  AlertTriangle,
  Clock,
  Sun,
  Sunset,
  Moon,
  ExternalLink,
  User,
  RotateCcw,
} from 'lucide-react';

interface PunchesViewProps {
  sites: AdminSite[];
}

export const PunchesView: React.FC<PunchesViewProps> = ({ sites }) => {
  const [attendance, setAttendance] = useState<AdminDailyAttendance[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedSite, setSelectedSite] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [selectedShift, setSelectedShift] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');

  // Load employee roster for the filter dropdown
  useEffect(() => {
    api.getUsers().then(setUsers).catch(console.error);
  }, []);

  const loadAttendance = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getAttendance({
        from_date: fromDate || undefined,
        to_date: toDate || undefined,
        site_code: selectedSite || undefined,
        user_id: employeeId || undefined,
        shift_type: selectedShift || undefined,
        status: selectedStatus || undefined,
      });
      setAttendance(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load attendance records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAttendance();
  }, [fromDate, toDate, selectedSite, employeeId, selectedShift, selectedStatus]);

  const handleExportCsv = async () => {
    try {
      setExporting(true);
      await api.downloadAttendanceCsv({
        from_date: fromDate || undefined,
        to_date: toDate || undefined,
        site_code: selectedSite || undefined,
        user_id: employeeId || undefined,
        shift_type: selectedShift || undefined,
        status: selectedStatus || undefined,
      });
    } catch (err: any) {
      alert(`Export failed: ${err.message}`);
    } finally {
      setExporting(false);
    }
  };

  const handleResetFilters = () => {
    setFromDate('');
    setToDate('');
    setSelectedSite('');
    setEmployeeId('');
    setSelectedShift('');
    setSelectedStatus('');
  };

  const filteredAttendance = attendance;

  const selectedUser = users.find((u) => u.user_id === employeeId);

  // Stat computations
  const totalDays = filteredAttendance.length;
  const completeDays = filteredAttendance.filter((a) => a.status === 'COMPLETE').length;
  const inProgressDays = filteredAttendance.filter((a) => a.status === 'IN_PROGRESS').length;
  const missedDays = filteredAttendance.filter((a) => a.status === 'MISSED_PUNCH').length;

  const renderShiftBadge = (shiftType: string) => {
    switch (shiftType) {
      case 'EVENING':
        return (
          <span className="badge badge-shift-evening" title="Evening Shift (14:00 - 23:00)">
            <Sunset size={12} /> Evening Shift
          </span>
        );
      case 'NIGHT':
        return (
          <span className="badge badge-shift-night" title="Night Shift (21:00 - 06:00)">
            <Moon size={12} /> Night Shift
          </span>
        );
      default:
        return (
          <span className="badge badge-shift-general" title="General Shift (09:00 - 18:00)">
            <Sun size={12} /> Day Shift
          </span>
        );
    }
  };

  const renderStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETE':
        return (
          <span className="badge badge-complete">
            <CheckCircle size={12} /> Complete
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="badge badge-in-progress">
            <Clock size={12} /> In Progress
          </span>
        );
      case 'MISSED_PUNCH':
        return (
          <span className="badge badge-missed">
            <AlertTriangle size={12} /> Missed Punch
          </span>
        );
      default:
        return <span className="badge badge-employee">{status}</span>;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Overview Metric Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}>
            <Clock size={24} />
          </div>
          <div>
            <div className="stat-value">{totalDays}</div>
            <div className="stat-label">Total Days Recorded</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--success-bg)', color: 'var(--success)' }}>
            <CheckCircle size={24} />
          </div>
          <div>
            <div className="stat-value">{completeDays}</div>
            <div className="stat-label">Complete (In + Out)</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#eff6ff', color: '#2563eb' }}>
            <Clock size={24} />
          </div>
          <div>
            <div className="stat-value">{inProgressDays}</div>
            <div className="stat-label">Active / In Progress</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--warning-bg)', color: 'var(--warning)' }}>
            <AlertTriangle size={24} />
          </div>
          <div>
            <div className="stat-value">{missedDays}</div>
            <div className="stat-label">Missed Punches</div>
          </div>
        </div>
      </div>

      {/* Main Attendance Table Card */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Daily Attendance Records</h2>
            <p className="card-subtitle">
              Consolidated view with Punch In & Punch Out paired per work date and shift
            </p>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={handleExportCsv}
              disabled={exporting}
              className="btn btn-success"
              title="Download CSV report"
            >
              <Download size={16} />{' '}
              {exporting
                ? 'Generating CSV...'
                : selectedUser
                ? `Export CSV (${selectedUser.name})`
                : 'Export All Attendance CSV'}
            </button>
            <button
              onClick={loadAttendance}
              disabled={loading}
              className="btn btn-secondary"
              title="Refresh attendance list"
            >
              <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>

        {/* Filter Controls Bar */}
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Employee</label>
            <select
              className="select"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
            >
              <option value="">All Employees</option>
              {users.map((u) => (
                <option key={u.user_id} value={u.user_id}>
                  {u.name} ({u.user_id})
                </option>
              ))}
            </select>
          </div>

          <div className="filter-group">
            <label className="filter-label">Shift</label>
            <select
              className="select"
              value={selectedShift}
              onChange={(e) => setSelectedShift(e.target.value)}
            >
              <option value="">All Shifts</option>
              <option value="GENERAL">Day Shift (09:00 - 18:00)</option>
              <option value="EVENING">Evening Shift (14:00 - 23:00)</option>
              <option value="NIGHT">Night Shift (21:00 - 06:00)</option>
            </select>
          </div>

          <div className="filter-group">
            <label className="filter-label">From Date</label>
            <input
              type="date"
              className="input"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>

          <div className="filter-group">
            <label className="filter-label">To Date</label>
            <input
              type="date"
              className="input"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>

          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select
              className="select"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="COMPLETE">Complete (In + Out)</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="MISSED_PUNCH">Missed Punch</option>
            </select>
          </div>

          <div className="filter-group">
            <label className="filter-label">Location / Site</label>
            <select
              className="select"
              value={selectedSite}
              onChange={(e) => setSelectedSite(e.target.value)}
            >
              <option value="">All Locations & Sites</option>
              {sites.map((s) => (
                <option key={s.office_id} value={s.office_id}>
                  🏢 Office: {s.name} ({s.office_id})
                </option>
              ))}
              <option value="NEW_LOCATION">📍 New Location / Field Visit</option>
            </select>
          </div>

          {(fromDate || toDate || selectedSite || employeeId || selectedShift || selectedStatus) && (
            <div style={{ display: 'flex', alignItems: 'flex-end' }}>
              <button
                onClick={handleResetFilters}
                className="btn btn-secondary btn-sm"
                title="Reset all filters"
                style={{ height: 38 }}
              >
                <RotateCcw size={14} /> Clear
              </button>
            </div>
          )}
        </div>

        {error && (
          <div style={{ padding: '16px 20px', background: 'var(--error-bg)', color: 'var(--error)', borderBottom: '1px solid var(--border)', fontSize: '0.9rem' }}>
            {error}
          </div>
        )}

        {/* Consolidated Daily Table */}
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date & Shift</th>
                <th>Employee</th>
                <th>Punch In</th>
                <th>Punch Out</th>
                <th>Work Hours</th>
                <th>Location</th>
                <th>Status</th>
                <th>Face Match</th>
              </tr>
            </thead>
            <tbody>
              {filteredAttendance.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '48px 20px', color: 'var(--text-secondary)' }}>
                    {loading ? 'Loading attendance records...' : 'No attendance records found matching filter criteria.'}
                  </td>
                </tr>
              ) : (
                filteredAttendance.map((item) => {
                  const isOffice = item.location_type === 'OFFICE';
                  return (
                    <tr key={`${item.user_id}_${item.date_local}`}>
                      {/* Date & Shift */}
                      <td>
                        <div style={{ fontWeight: 700 }}>{item.date_local}</div>
                        <div style={{ marginTop: 4 }}>{renderShiftBadge(item.shift_type)}</div>
                      </td>

                      {/* Employee */}
                      <td>
                        <div style={{ fontWeight: 700 }}>{item.user_name}</div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                          {item.user_id} &bull; {item.site_code}
                        </div>
                      </td>

                      {/* Punch In */}
                      <td>
                        {item.in_time_local ? (
                          <div>
                            <div style={{ fontWeight: 600, color: '#065f46' }}>{item.in_time_local}</div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>Check-in</div>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-secondary)' }}>—</span>
                        )}
                      </td>

                      {/* Punch Out */}
                      <td>
                        {item.out_time_local ? (
                          <div>
                            <div style={{ fontWeight: 600, color: '#1e40af' }}>{item.out_time_local}</div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>Check-out</div>
                          </div>
                        ) : item.status === 'IN_PROGRESS' ? (
                          <span style={{ fontStyle: 'italic', color: 'var(--primary)', fontSize: '0.82rem' }}>
                            ⏳ In Progress
                          </span>
                        ) : (
                          <span style={{ color: 'var(--text-secondary)' }}>—</span>
                        )}
                      </td>

                      {/* Work Hours Duration */}
                      <td>
                        <div style={{ fontWeight: 700, fontSize: '0.92rem' }}>
                          {item.work_duration_formatted}
                        </div>
                        {item.work_duration_minutes != null && (
                          <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                            {item.work_duration_minutes} mins
                          </div>
                        )}
                      </td>

                      {/* Location (Unified single column) */}
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                          <span className={`badge ${isOffice ? 'badge-office' : 'badge-new-location'}`}>
                            {isOffice ? '🏢 Office' : '📍 New Location'}
                          </span>
                          {item.latitude != null && item.longitude != null && (
                            <a
                              href={item.maps_url || `https://www.google.com/maps/search/?api=1&query=${item.latitude},${item.longitude}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="map-link"
                              style={{ fontSize: '0.76rem' }}
                            >
                              <MapPin size={11} /> {item.latitude.toFixed(4)}, {item.longitude.toFixed(4)}
                            </a>
                          )}
                        </div>
                      </td>

                      {/* Status */}
                      <td>{renderStatusBadge(item.status)}</td>

                      {/* Face Match Verification */}
                      <td>
                        {item.match_score_avg != null ? (
                          <div>
                            <div
                              style={{
                                fontWeight: 700,
                                fontSize: '0.85rem',
                                color: item.match_score_avg >= 0.6 ? 'var(--success)' : 'var(--warning)',
                              }}
                            >
                              {(item.match_score_avg * 100).toFixed(0)}%
                            </div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>Verified</div>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-secondary)' }}>—</span>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
