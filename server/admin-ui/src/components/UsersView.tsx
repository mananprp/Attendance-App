import React, { useState, useEffect } from 'react';
import { api, getStoredUser } from '../api';
import { AdminUser, AdminSite } from '../types';
import { UserPlus, Shield, User, MapPin, RefreshCw, X, Key, Trash2, CheckCircle, AlertCircle } from 'lucide-react';

interface UsersViewProps {
  sites: AdminSite[];
}

export const UsersView: React.FC<UsersViewProps> = ({ sites }) => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const currentUser = getStoredUser();

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

  // Reset Password Modal State
  const [resettingUser, setResettingUser] = useState<AdminUser | null>(null);
  const [newPasswordForReset, setNewPasswordForReset] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);
  const [resetPasswordSuccess, setResetPasswordSuccess] = useState<string | null>(null);
  const [resetPasswordError, setResetPasswordError] = useState<string | null>(null);

  // Delete User Modal State
  const [deletingUser, setDeletingUser] = useState<AdminUser | null>(null);
  const [deleting, setDeleting] = useState(false);

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

  const handleOpenResetPasswordModal = (user: AdminUser) => {
    setResettingUser(user);
    setNewPasswordForReset('');
    setResetPasswordSuccess(null);
    setResetPasswordError(null);
  };

  const handleSavePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resettingUser) return;
    setResetPasswordError(null);
    setResetPasswordSuccess(null);

    if (newPasswordForReset.length < 6) {
      setResetPasswordError('Password must be at least 6 characters.');
      return;
    }
    try {
      setSavingPassword(true);
      await api.updateUserPassword(resettingUser.user_id, newPasswordForReset);
      setResetPasswordSuccess(`Password for ${resettingUser.name} (${resettingUser.user_id}) updated successfully!`);
      setTimeout(() => {
        setResettingUser(null);
        setNewPasswordForReset('');
        setResetPasswordSuccess(null);
        setResetPasswordError(null);
      }, 1800);
    } catch (err: any) {
      setResetPasswordError(err.message || 'Failed to update password');
    } finally {
      setSavingPassword(false);
    }
  };

  const handleOpenDeleteModal = (user: AdminUser) => {
    setDeletingUser(user);
  };

  const handleConfirmDelete = async () => {
    if (!deletingUser) return;
    try {
      setDeleting(true);
      await api.deleteUser(deletingUser.user_id);
      setDeletingUser(null);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to delete user: ${err.message}`);
    } finally {
      setDeleting(false);
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
                    <div style={{ display: 'inline-flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
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
                      <button
                        onClick={() => handleOpenResetPasswordModal(u)}
                        className="btn btn-secondary btn-sm"
                        title="Reset employee password"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                      >
                        <Key size={13} /> Password
                      </button>
                      <button
                        onClick={() => handleOpenDeleteModal(u)}
                        disabled={currentUser?.userId === u.user_id}
                        className="btn btn-sm"
                        style={{
                          background: 'rgba(239, 68, 68, 0.1)',
                          color: '#ef4444',
                          border: '1px solid rgba(239, 68, 68, 0.2)',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 4,
                          cursor: currentUser?.userId === u.user_id ? 'not-allowed' : 'pointer',
                          opacity: currentUser?.userId === u.user_id ? 0.5 : 1,
                        }}
                        title={currentUser?.userId === u.user_id ? 'Cannot delete own account' : 'Delete user account'}
                      >
                        <Trash2 size={13} /> Delete
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

      {/* Reset Password Modal */}
      {resettingUser && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                Reset Password: {resettingUser.name} ({resettingUser.user_id})
              </h3>
              <button
                onClick={() => {
                  setResettingUser(null);
                  setResetPasswordSuccess(null);
                  setResetPasswordError(null);
                }}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSavePassword}>
              <div className="modal-body">
                {resetPasswordSuccess && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      backgroundColor: 'var(--success-bg)',
                      color: '#065f46',
                      border: '1px solid #a7f3d0',
                      padding: '12px 14px',
                      borderRadius: 8,
                      marginBottom: 16,
                      fontSize: '0.88rem',
                      fontWeight: 500,
                    }}
                  >
                    <CheckCircle size={18} color="#10b981" />
                    <span>{resetPasswordSuccess}</span>
                  </div>
                )}

                {resetPasswordError && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      backgroundColor: 'var(--error-bg)',
                      color: '#991b1b',
                      border: '1px solid #fecaca',
                      padding: '12px 14px',
                      borderRadius: 8,
                      marginBottom: 16,
                      fontSize: '0.88rem',
                      fontWeight: 500,
                    }}
                  >
                    <AlertCircle size={18} color="#ef4444" />
                    <span>{resetPasswordError}</span>
                  </div>
                )}

                {!resetPasswordSuccess && (
                  <>
                    <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: 16 }}>
                      Enter a new password for this user. The password must be at least 6 characters.
                    </p>
                    <div className="filter-group">
                      <label className="filter-label">New Password</label>
                      <input
                        type="password"
                        className="input"
                        placeholder="Enter new password (min 6 chars)"
                        value={newPasswordForReset}
                        onChange={(e) => {
                          setNewPasswordForReset(e.target.value);
                          if (resetPasswordError) setResetPasswordError(null);
                        }}
                        required
                        minLength={6}
                        autoFocus
                        disabled={savingPassword}
                      />
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => {
                    setResettingUser(null);
                    setResetPasswordSuccess(null);
                    setResetPasswordError(null);
                  }}
                  className="btn btn-secondary"
                  disabled={savingPassword}
                >
                  {resetPasswordSuccess ? 'Close' : 'Cancel'}
                </button>
                {!resetPasswordSuccess && (
                  <button
                    type="submit"
                    disabled={savingPassword || newPasswordForReset.length < 6}
                    className="btn btn-primary"
                  >
                    {savingPassword ? 'Updating...' : 'Update Password'}
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete User Confirmation Modal */}
      {deletingUser && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: 440 }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ef4444' }}>
                Confirm Delete Employee
              </h3>
              <button
                onClick={() => setDeletingUser(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)', marginBottom: 12 }}>
                Are you sure you want to permanently delete <strong>{deletingUser.name}</strong> (<code>{deletingUser.user_id}</code>)?
              </p>
              <div style={{ padding: 12, borderRadius: 8, background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.2)', fontSize: '0.82rem', color: '#b91c1c' }}>
                ⚠️ Warning: This will delete the user account and associated punch records. This action cannot be undone.
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                onClick={() => setDeletingUser(null)}
                disabled={deleting}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmDelete}
                disabled={deleting}
                className="btn"
                style={{
                  background: '#ef4444',
                  color: '#fff',
                  border: 'none',
                }}
              >
                {deleting ? 'Deleting...' : 'Delete Permanently'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
