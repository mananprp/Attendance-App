import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { AdminUser, AdminSite } from '../types';
import { UserPlus, Shield, User, MapPin, RefreshCw, X } from 'lucide-react';

interface UsersViewProps {
  sites: AdminSite[];
}

export const UsersView: React.FC<UsersViewProps> = ({ sites }) => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Create User Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newUserId, setNewUserId] = useState('');
  const [newName, setNewName] = useState('');
  const [newPassword, setNewPassword] = useState('Password@123');
  const [newRole, setNewRole] = useState<'EMPLOYEE' | 'ADMIN'>('EMPLOYEE');
  const [newSiteCode, setNewSiteCode] = useState('DEFAULT_OFFICE');
  const [creating, setCreating] = useState(false);

  // Edit Site Modal State
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [selectedSiteCode, setSelectedSiteCode] = useState('');
  const [savingSite, setSavingSite] = useState(false);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getUsers();
      setUsers(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load employees');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCreating(true);
      await api.createUser({
        user_id: newUserId.trim().toUpperCase(),
        name: newName.trim(),
        password: newPassword,
        role: newRole,
        site_code: newSiteCode,
      });
      setShowCreateModal(false);
      setNewUserId('');
      setNewName('');
      setNewPassword('Password@123');
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to create employee: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  const handleUpdateRole = async (user: AdminUser) => {
    const nextRole = user.role === 'ADMIN' ? 'EMPLOYEE' : 'ADMIN';
    const confirm = window.confirm(
      `Are you sure you want to change role of ${user.name} (${user.user_id}) to ${nextRole}?`,
    );
    if (!confirm) return;

    try {
      await api.updateUserRole(user.user_id, nextRole);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to update role: ${err.message}`);
    }
  };

  const handleOpenSiteModal = (user: AdminUser) => {
    setEditingUser(user);
    setSelectedSiteCode(user.site_code);
  };

  const handleSaveUserSite = async () => {
    if (!editingUser) return;
    try {
      setSavingSite(true);
      await api.updateUserSite(editingUser.user_id, selectedSiteCode);
      setEditingUser(null);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to update assigned site: ${err.message}`);
    } finally {
      setSavingSite(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">Employee Directory</h2>
            <p className="card-subtitle">Manage user accounts, roles, and assigned office site locations</p>
          </div>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary"
            >
              <UserPlus size={16} /> Add New Employee
            </button>
            <button
              onClick={loadUsers}
              disabled={loading}
              className="btn btn-secondary"
            >
              <RefreshCw size={16} className={loading ? 'spin' : ''} /> Refresh
            </button>
          </div>
        </div>

        {error && (
          <div style={{ padding: '16px 24px', background: 'var(--error-bg)', color: 'var(--error)', fontSize: '0.88rem' }}>
            {error}
          </div>
        )}

        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Employee ID</th>
                <th>Full Name</th>
                <th>Role</th>
                <th>Assigned Office Site</th>
                <th>Status</th>
                <th>Profile Photo</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.user_id}>
                  <td>
                    <span style={{ fontWeight: 700, fontFamily: 'monospace' }}>{u.user_id}</span>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{u.name}</div>
                  </td>
                  <td>
                    <span className={`badge ${u.role === 'ADMIN' ? 'badge-admin' : 'badge-employee'}`}>
                      {u.role === 'ADMIN' ? (
                        <>
                          <Shield size={12} /> ADMIN
                        </>
                      ) : (
                        <>
                          <User size={12} /> EMPLOYEE
                        </>
                      )}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <MapPin size={14} color="var(--primary)" />
                      <span>{u.site_name}</span>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>({u.site_code})</span>
                    </div>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.8rem', color: 'var(--success)', fontWeight: 600 }}>Active</span>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.8rem', color: u.photo_change_used ? 'var(--text-secondary)' : 'var(--primary)' }}>
                      {u.photo_change_used ? 'Enrolled (v2 used)' : 'Enrolled'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: 8 }}>
                      <button
                        onClick={() => handleOpenSiteModal(u)}
                        className="btn btn-secondary btn-sm"
                        title="Reassign to another office site"
                      >
                        Change Site
                      </button>
                      <button
                        onClick={() => handleUpdateRole(u)}
                        className="btn btn-secondary btn-sm"
                        title="Toggle Admin / Employee role"
                      >
                        {u.role === 'ADMIN' ? 'Set Employee' : 'Promote Admin'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create User Modal */}
      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Create New Employee Account</h3>
              <button
                onClick={() => setShowCreateModal(false)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateUser}>
              <div className="modal-body">
                <div className="filter-group">
                  <label className="filter-label">Employee ID</label>
                  <input
                    type="text"
                    className="input"
                    placeholder="e.g. EMP1004"
                    value={newUserId}
                    onChange={(e) => setNewUserId(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Full Name</label>
                  <input
                    type="text"
                    className="input"
                    placeholder="e.g. David Miller"
                    value={newName}
                    onChange={(e) => setNewName(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Password</label>
                  <input
                    type="password"
                    className="input"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Role</label>
                  <select
                    className="select"
                    value={newRole}
                    onChange={(e) => setNewRole(e.target.value as any)}
                  >
                    <option value="EMPLOYEE">EMPLOYEE</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Assigned Office Site</label>
                  <select
                    className="select"
                    value={newSiteCode}
                    onChange={(e) => setNewSiteCode(e.target.value)}
                  >
                    {sites.map((s) => (
                      <option key={s.office_id} value={s.office_id}>
                        {s.name} ({s.office_id})
                      </option>
                    ))}
                  </select>
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
                  {creating ? 'Creating...' : 'Create Employee'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Site Assignment Modal */}
      {editingUser && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                Reassign Site: {editingUser.name} ({editingUser.user_id})
              </h3>
              <button
                onClick={() => setEditingUser(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="filter-group">
                <label className="filter-label">Select Target Office Site</label>
                <select
                  className="select"
                  value={selectedSiteCode}
                  onChange={(e) => setSelectedSiteCode(e.target.value)}
                >
                  {sites.map((s) => (
                    <option key={s.office_id} value={s.office_id}>
                      {s.name} ({s.office_id})
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                onClick={() => setEditingUser(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveUserSite}
                disabled={savingSite}
                className="btn btn-primary"
              >
                {savingSite ? 'Saving...' : 'Save Site Assignment'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
