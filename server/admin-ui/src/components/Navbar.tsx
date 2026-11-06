import React from 'react';
import { AuthSession } from '../types';
import { clearSession } from '../api';
import { Building2, Users, Clock, LogOut, Shield, Navigation } from 'lucide-react';

interface NavbarProps {
  session: AuthSession;
  activeTab: 'punches' | 'users' | 'sites' | 'tracking';
  onSelectTab: (tab: 'punches' | 'users' | 'sites' | 'tracking') => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ session, activeTab, onSelectTab, onLogout }) => {
  const handleSignOut = () => {
    clearSession();
    onLogout();
  };

  return (
    <header className="navbar">
      <div className="nav-container">
        <div className="nav-brand">
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 10,
              background: 'var(--primary)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Shield size={20} />
          </div>
          <span>Attendance Admin</span>
        </div>

        <nav className="nav-tabs">
          <button
            className={`nav-tab ${activeTab === 'punches' ? 'active' : ''}`}
            onClick={() => onSelectTab('punches')}
          >
            <Clock size={18} />
            <span>Daily Attendance</span>
          </button>
          <button
            className={`nav-tab ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => onSelectTab('users')}
          >
            <Users size={18} />
            <span>Employees</span>
          </button>
          <button
            className={`nav-tab ${activeTab === 'sites' ? 'active' : ''}`}
            onClick={() => onSelectTab('sites')}
          >
            <Building2 size={18} />
            <span>Office Sites</span>
          </button>
          <button
            className={`nav-tab ${activeTab === 'tracking' ? 'active' : ''}`}
            onClick={() => onSelectTab('tracking')}
          >
            <Navigation size={18} />
            <span>Field Tracking</span>
          </button>
        </nav>

        <div className="nav-profile">
          <div className="user-badge">
            <span className="user-badge-name">{session.user.name || session.user.userId}</span>
            <span className="user-badge-role">{session.user.role} ({session.user.userId})</span>
          </div>
          <button
            onClick={handleSignOut}
            className="btn btn-secondary btn-sm"
            title="Sign Out"
            style={{ color: 'var(--error)' }}
          >
            <LogOut size={16} /> Sign Out
          </button>
        </div>
      </div>
    </header>
  );
};
