import React, { useState, useEffect } from 'react';
import { api, getStoredUser } from '../api';
import { AdminUser, AdminSite } from '../types';
import { UserPlus, Shield, User, MapPin, RefreshCw, X, Key, Trash2, CheckCircle, AlertCircle, Camera, UserCheck, Edit2 } from 'lucide-react';

interface UsersViewProps {
  sites: AdminSite[];
}

export const UsersView: React.FC<UsersViewProps> = ({ sites }) => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [currentUser, setCurrentUser] = useState(getStoredUser());
  const isSupervisor = currentUser?.role === 'SUPERVISOR';

  // Create User Modal State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newUserId, setNewUserId] = useState('');
  const [newName, setNewName] = useState('');
  const [newPassword, setNewPassword] = useState('Password@123');
  const [newRole, setNewRole] = useState<'EMPLOYEE' | 'SUPERVISOR' | 'ADMIN'>('EMPLOYEE');
  const [newSiteCode, setNewSiteCode] = useState('DEFAULT_OFFICE');
  const [newSiteCodes, setNewSiteCodes] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);

  // Edit Site Modal State
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [selectedSiteCode, setSelectedSiteCode] = useState('');
  const [selectedSiteCodes, setSelectedSiteCodes] = useState<string[]>([]);
  const [savingSite, setSavingSite] = useState(false);

  // Full Edit User Modal State
  const [editingFullUser, setEditingFullUser] = useState<AdminUser | null>(null);
  const [editUserName, setEditUserName] = useState('');
  const [editUserRole, setEditUserRole] = useState<'EMPLOYEE' | 'SUPERVISOR' | 'ADMIN'>('EMPLOYEE');
  const [editUserSiteCodes, setEditUserSiteCodes] = useState<string[]>([]);
  const [savingFullUser, setSavingFullUser] = useState(false);

  // Edit Role Modal State
  const [roleUser, setRoleUser] = useState<AdminUser | null>(null);
  const [selectedRole, setSelectedRole] = useState<'EMPLOYEE' | 'SUPERVISOR' | 'ADMIN'>('EMPLOYEE');
  const [savingRole, setSavingRole] = useState(false);

  // Reset Password Modal State
  const [resettingUser, setResettingUser] = useState<AdminUser | null>(null);
  const [newPasswordForReset, setNewPasswordForReset] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);
  const [resetPasswordSuccess, setResetPasswordSuccess] = useState<string | null>(null);
  const [resetPasswordError, setResetPasswordError] = useState<string | null>(null);

  // Delete User Modal State
  const [deletingUser, setDeletingUser] = useState<AdminUser | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Reset Photo Modal State
  const [resettingPhotoUser, setResettingPhotoUser] = useState<AdminUser | null>(null);
  const [resettingPhoto, setResettingPhoto] = useState(false);
  const [resetPhotoSuccess, setResetPhotoSuccess] = useState<string | null>(null);
  const [resetPhotoError, setResetPhotoError] = useState<string | null>(null);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getUsers();
      setUsers(data);

      if (currentUser?.userId) {
        const myRecord = data.find((u) => u.user_id.toUpperCase() === currentUser.userId.toUpperCase());
        if (myRecord && myRecord.role !== currentUser.role) {
          const updated = { ...currentUser, role: myRecord.role };
          localStorage.setItem('admin_user', JSON.stringify(updated));
          setCurrentUser(updated);
        }
      }
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
    if (isSupervisor && newRole === 'ADMIN') {
      alert('Supervisors cannot create Admin users.');
      return;
    }
    try {
      setCreating(true);
      const targetSites = newSiteCodes.length > 0 ? newSiteCodes : [newSiteCode];
      await api.createUser({
        user_id: newUserId.trim().toUpperCase(),
        name: newName.trim(),
        password: newPassword,
        role: newRole,
        site_code: targetSites[0],
        site_codes: targetSites,
      });
      setShowCreateModal(false);
      setNewUserId('');
      setNewName('');
      setNewPassword('Password@123');
      setNewSiteCodes([]);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to create employee: ${err.message}`);
    } finally {
      setCreating(false);
    }
  };

  const handleOpenRoleModal = (user: AdminUser) => {
    if (isSupervisor && user.role === 'ADMIN') {
      alert('Supervisors cannot modify Administrator accounts.');
      return;
    }
    setRoleUser(user);
    setSelectedRole(isSupervisor && user.role === 'ADMIN' ? 'EMPLOYEE' : user.role);
  };

  const handleSaveUserRole = async () => {
    if (!roleUser) return;
    if (isSupervisor && selectedRole === 'ADMIN') {
      alert('Supervisors cannot assign the Admin role.');
      return;
    }
    try {
      setSavingRole(true);
      await api.updateUserRole(roleUser.user_id, selectedRole);
      setRoleUser(null);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to update role: ${err.message}`);
    } finally {
      setSavingRole(false);
    }
  };

  const handleOpenSiteModal = (user: AdminUser) => {
    setEditingUser(user);
    const existing = user.assigned_sites?.map((s) => s.site_code) || [user.site_code];
    setSelectedSiteCodes(existing.length > 0 ? existing : [user.site_code]);
    setSelectedSiteCode(user.site_code);
  };

  const handleSaveUserSite = async () => {
    if (!editingUser) return;
    try {
      setSavingSite(true);
      const primary = selectedSiteCodes[0] || selectedSiteCode;
      await api.updateUserSite(editingUser.user_id, primary, selectedSiteCodes);
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

  const handleOpenEditFullUser = (user: AdminUser) => {
    if (isSupervisor && user.role === 'ADMIN') {
      alert('Supervisors cannot modify Administrator accounts.');
      return;
    }
    setEditingFullUser(user);
    setEditUserName(user.name);
    setEditUserRole(user.role);
    const existing = user.assigned_sites?.map((s) => s.site_code) || [user.site_code];
    setEditUserSiteCodes(existing.length > 0 ? existing : [user.site_code]);
  };

  const handleSaveFullUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingFullUser) return;
    if (isSupervisor && editUserRole === 'ADMIN') {
      alert('Supervisors cannot assign the Admin role.');
      return;
    }
    if (editUserSiteCodes.length === 0) {
      alert('Please select at least one assigned office site.');
      return;
    }
    try {
      setSavingFullUser(true);
      await api.updateUser(editingFullUser.user_id, {
        name: editUserName.trim(),
        role: editUserRole,
        site_code: editUserSiteCodes[0],
        site_codes: editUserSiteCodes,
      });
      setEditingFullUser(null);
      await loadUsers();
    } catch (err: any) {
      alert(`Failed to update employee details: ${err.message}`);
    } finally {
      setSavingFullUser(false);
    }
  };

  const handleOpenDeleteModal = (user: AdminUser) => {
    setDeletingUser(user);
  };

  const handleOpenResetPhotoModal = (user: AdminUser) => {
    setResettingPhotoUser(user);
    setResetPhotoSuccess(null);
    setResetPhotoError(null);
  };

  const handleConfirmResetPhoto = async () => {
    if (!resettingPhotoUser) return;
    try {
      setResettingPhoto(true);
      setResetPhotoError(null);
      const res = await api.resetUserPhoto(resettingPhotoUser.user_id);
      setResetPhotoSuccess(res.message || 'Photo reset successfully');
      setTimeout(async () => {
        setResettingPhotoUser(null);
        setResetPhotoSuccess(null);
        await loadUsers();
      }, 1500);
    } catch (err: any) {
      setResetPhotoError(err.message || 'Failed to reset photo');
    } finally {
      setResettingPhoto(false);
    }
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
                    <span className={`badge ${u.role === 'ADMIN' ? 'badge-admin' : (u.role === 'SUPERVISOR' ? 'badge-in-progress' : 'badge-employee')}`}>
                      {u.role === 'ADMIN' ? (
                        <>
                          <Shield size={12} /> ADMIN
                        </>
                      ) : u.role === 'SUPERVISOR' ? (
                        <>
                          <UserCheck size={12} /> SUPERVISOR
                        </>
                      ) : (
                        <>
                          <User size={12} /> EMPLOYEE
                        </>
                      )}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <MapPin size={14} color="var(--primary)" />
                        <span style={{ fontWeight: 600 }}>{u.site_name}</span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>({u.site_code})</span>
                      </div>
                      {u.assigned_sites && u.assigned_sites.length > 1 && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--primary)', fontWeight: 500, paddingLeft: 20 }}>
                          +{u.assigned_sites.length - 1} more sites assigned
                        </div>
                      )}
                    </div>
                  </td>
                  <td>
                    <span style={{ fontSize: '0.8rem', color: 'var(--success)', fontWeight: 600 }}>Active</span>
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ fontSize: '0.8rem', color: u.photo_change_used ? 'var(--text-secondary)' : 'var(--primary)', fontWeight: 500 }}>
                        {u.photo_change_used ? 'Enrolled (v2 used)' : 'Enrolled'}
                      </span>
                      <button
                        onClick={() => handleOpenResetPhotoModal(u)}
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '2px 8px', fontSize: '0.72rem', display: 'inline-flex', alignItems: 'center', gap: 4 }}
                        title="Reset employee profile photo count to allow re-enrolment"
                      >
                        <Camera size={11} /> Reset
                      </button>
                    </div>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                      {u.role === 'ADMIN' && isSupervisor ? (
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontStyle: 'italic', padding: '4px 8px' }}>
                          Protected Admin
                        </span>
                      ) : (
                        <>
                          <button
                            onClick={() => handleOpenEditFullUser(u)}
                            className="btn btn-secondary btn-sm"
                            title="Edit employee name, role, and assigned sites"
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          >
                            <Edit2 size={13} /> Edit
                          </button>
                          <button
                            onClick={() => handleOpenSiteModal(u)}
                            className="btn btn-secondary btn-sm"
                            title="Reassign to another office site"
                          >
                            Change Site
                          </button>
                          <button
                            onClick={() => handleOpenRoleModal(u)}
                            className="btn btn-secondary btn-sm"
                            title="Change user access role (Supervisor, Employee)"
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          >
                            <Shield size={13} /> Change Role
                          </button>
                          {!isSupervisor && (
                            <button
                              onClick={() => handleOpenResetPasswordModal(u)}
                              className="btn btn-secondary btn-sm"
                              title="Reset employee password"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                            >
                              <Key size={13} /> Password
                            </button>
                          )}
                          <button
                            onClick={() => handleOpenResetPhotoModal(u)}
                            className="btn btn-secondary btn-sm"
                            title="Reset employee profile photo count"
                            style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          >
                            <Camera size={13} /> Reset Photo
                          </button>
                          {!isSupervisor && (
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
                          )}
                        </>
                      )}
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
                    <option value="SUPERVISOR">SUPERVISOR</option>
                    {!isSupervisor && (
                      <option value="ADMIN">ADMIN</option>
                    )}
                  </select>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Assigned Office Sites (Select 1 or more)</label>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 160, overflowY: 'auto', padding: 10, border: '1px solid var(--border)', borderRadius: 8, background: 'var(--card-bg)' }}>
                    {sites.map((s) => {
                      const checked = newSiteCodes.length > 0 ? newSiteCodes.includes(s.office_id) : s.office_id === newSiteCode;
                      return (
                        <label key={s.office_id} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.88rem' }}>
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={(e) => {
                              const currentList = newSiteCodes.length > 0 ? [...newSiteCodes] : [newSiteCode];
                              if (e.target.checked) {
                                if (!currentList.includes(s.office_id)) currentList.push(s.office_id);
                              } else {
                                const idx = currentList.indexOf(s.office_id);
                                if (idx >= 0 && currentList.length > 1) currentList.splice(idx, 1);
                              }
                              setNewSiteCodes(currentList);
                              setNewSiteCode(currentList[0]);
                            }}
                          />
                          <span style={{ fontWeight: 600 }}>{s.name}</span>
                          <span style={{ color: 'var(--text-secondary)', fontSize: '0.78rem' }}>({s.office_id})</span>
                        </label>
                      );
                    })}
                  </div>
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
                Manage Sites: {editingUser.name} ({editingUser.user_id})
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
                <label className="filter-label">Assigned Office Sites (Select all applicable locations)</label>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
                  Employees assigned to multiple sites can check in at any of these office geofences.
                </p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 220, overflowY: 'auto', padding: 10, border: '1px solid var(--border)', borderRadius: 8, background: 'var(--card-bg)' }}>
                  {sites.map((s) => {
                    const checked = selectedSiteCodes.includes(s.office_id);
                    return (
                      <label key={s.office_id} style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.88rem' }}>
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedSiteCodes([...selectedSiteCodes, s.office_id]);
                            } else {
                              if (selectedSiteCodes.length > 1) {
                                setSelectedSiteCodes(selectedSiteCodes.filter((code) => code !== s.office_id));
                              }
                            }
                          }}
                        />
                        <span style={{ fontWeight: 600 }}>{s.name}</span>
                        <span style={{ color: 'var(--text-secondary)', fontSize: '0.78rem' }}>({s.office_id})</span>
                      </label>
                    );
                  })}
                </div>
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

      {/* Change Role Modal */}
      {roleUser && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
                <Shield size={20} className="text-accent" />
                Change Role: {roleUser.name} ({roleUser.user_id})
              </h3>
              <button
                onClick={() => setRoleUser(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="filter-group">
                <label className="filter-label">Select System Role</label>
                <select
                  className="select"
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value as any)}
                  style={{ width: '100%', marginBottom: 12 }}
                >
                  <option value="EMPLOYEE">EMPLOYEE</option>
                  <option value="SUPERVISOR">SUPERVISOR</option>
                  {!isSupervisor && (
                    <option value="ADMIN">ADMIN</option>
                  )}
                </select>

                <div style={{ background: 'var(--card-bg)', border: '1px solid var(--border)', borderRadius: 8, padding: 12, fontSize: '0.85rem' }}>
                  <div style={{ fontWeight: 600, marginBottom: 6, color: 'var(--text-primary)' }}>Role Permissions Summary:</div>
                  {selectedRole === 'EMPLOYEE' && (
                    <div style={{ color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      • Mark daily attendance check-in/out at assigned office site(s).<br/>
                      • Record field trips with live GPS tracking.<br/>
                      • View personal attendance logs and profile.
                    </div>
                  )}
                  {selectedRole === 'SUPERVISOR' && (
                    <div style={{ color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      • All Employee features.<br/>
                      • View staff attendance across assigned sites.<br/>
                      • Can manage office sites and oversee on-ground operations.
                    </div>
                  )}
                  {selectedRole === 'ADMIN' && (
                    <div style={{ color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      • Full administrator privileges across all office sites.<br/>
                      • Create, update, and delete users and sites.<br/>
                      • Assign user roles, reset passwords, and reset face photo verification.
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                onClick={() => setRoleUser(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveUserRole}
                disabled={savingRole || selectedRole === roleUser.role}
                className="btn btn-primary"
              >
                {savingRole ? 'Updating...' : `Set as ${selectedRole}`}
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
      {/* Reset Photo Confirmation Modal */}
      {resettingPhotoUser && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: 440 }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                Reset Photo: {resettingPhotoUser.name} ({resettingPhotoUser.user_id})
              </h3>
              <button
                onClick={() => {
                  setResettingPhotoUser(null);
                  setResetPhotoSuccess(null);
                  setResetPhotoError(null);
                }}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              {resetPhotoSuccess && (
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
                  <span>{resetPhotoSuccess}</span>
                </div>
              )}

              {resetPhotoError && (
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
                  <span>{resetPhotoError}</span>
                </div>
              )}

              {!resetPhotoSuccess && (
                <>
                  <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)', marginBottom: 12 }}>
                    Are you sure you want to reset the profile photo for <strong>{resettingPhotoUser.name}</strong> (<code>{resettingPhotoUser.user_id}</code>)?
                  </p>
                  <div style={{ padding: 12, borderRadius: 8, background: 'rgba(234, 179, 8, 0.1)', border: '1px solid rgba(234, 179, 8, 0.3)', fontSize: '0.82rem', color: '#854d0e' }}>
                    ℹ️ The user will be asked to re-enrol their profile photo at next login. Attendance days and punch records will not be affected.
                  </div>
                </>
              )}
            </div>
            <div className="modal-footer">
              <button
                type="button"
                onClick={() => {
                  setResettingPhotoUser(null);
                  setResetPhotoSuccess(null);
                  setResetPhotoError(null);
                }}
                disabled={resettingPhoto}
                className="btn btn-secondary"
              >
                {resetPhotoSuccess ? 'Close' : 'Cancel'}
              </button>
              {!resetPhotoSuccess && (
                <button
                  type="button"
                  onClick={handleConfirmResetPhoto}
                  disabled={resettingPhoto}
                  className="btn btn-primary"
                >
                  {resettingPhoto ? 'Resetting...' : 'Confirm Reset Photo'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Edit Full User Modal */}
      {editingFullUser && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Edit Employee Details</h3>
                <span style={{ fontSize: '0.78rem', fontFamily: 'monospace', color: 'var(--text-secondary)' }}>
                  ID: {editingFullUser.user_id} (Locked)
                </span>
              </div>
              <button
                onClick={() => setEditingFullUser(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSaveFullUser}>
              <div className="modal-body">
                <div className="filter-group">
                  <label className="filter-label">Full Name</label>
                  <input
                    type="text"
                    className="input"
                    value={editUserName}
                    onChange={(e) => setEditUserName(e.target.value)}
                    required
                  />
                </div>

                <div className="filter-group">
                  <label className="filter-label">Access Role</label>
                  <select
                    className="select"
                    value={editUserRole}
                    onChange={(e) => setEditUserRole(e.target.value as any)}
                  >
                    <option value="EMPLOYEE">EMPLOYEE (Standard Access)</option>
                    <option value="SUPERVISOR">SUPERVISOR (Site Management)</option>
                    {!isSupervisor && <option value="ADMIN">ADMIN (Full Console Access)</option>}
                  </select>
                </div>

                <div className="filter-group">
                  <label className="filter-label">Assigned Office Site Locations</label>
                  <div
                    style={{
                      maxHeight: 180,
                      overflowY: 'auto',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      padding: 10,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8,
                      background: '#fff',
                    }}
                  >
                    {sites.map((s) => {
                      const checked = editUserSiteCodes.includes(s.office_id);
                      return (
                        <label
                          key={s.office_id}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            fontSize: '0.88rem',
                            cursor: 'pointer',
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={checked}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setEditUserSiteCodes([...editUserSiteCodes, s.office_id]);
                              } else {
                                if (editUserSiteCodes.length > 1) {
                                  setEditUserSiteCodes(editUserSiteCodes.filter((c) => c !== s.office_id));
                                } else {
                                  alert('Employee must be assigned to at least one site.');
                                }
                              }
                            }}
                          />
                          <div>
                            <span style={{ fontWeight: 600 }}>{s.name}</span>
                            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginLeft: 6 }}>
                              ({s.office_id})
                            </span>
                          </div>
                        </label>
                      );
                    })}
                  </div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: 4 }}>
                    The first selected site serves as the primary office site.
                  </span>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  onClick={() => setEditingFullUser(null)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingFullUser || !editUserName.trim()}
                  className="btn btn-primary"
                >
                  {savingFullUser ? 'Saving...' : 'Save Employee Details'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
