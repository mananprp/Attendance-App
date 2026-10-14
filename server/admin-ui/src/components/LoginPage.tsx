import React, { useState } from 'react';
import { api } from '../api';
import { AuthSession } from '../types';
import { ShieldCheck, LogIn, AlertCircle } from 'lucide-react';

interface LoginPageProps {
  onLoginSuccess: (session: AuthSession) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const [userId, setUserId] = useState('EMP1001');
  const [password, setPassword] = useState('Password@123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userId.trim() || !password.trim()) {
      setError('Please provide Employee ID and password');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const session = await api.login(userId.trim().toUpperCase(), password);
      onLoginSuccess(session);
    } catch (err: any) {
      setError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickFill = (id: string) => {
    setUserId(id);
    setPassword('Password@123');
    setError(null);
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-logo">
          <ShieldCheck size={32} />
        </div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: 6 }}>
          Admin Portal
        </h1>
        <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: 24 }}>
          Attendance System Management Console
        </p>

        {error && (
          <div
            style={{
              padding: '12px 16px',
              borderRadius: 8,
              background: 'var(--error-bg)',
              color: 'var(--error)',
              fontSize: '0.85rem',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              marginBottom: 20,
              border: '1px solid #fecaca',
            }}
          >
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="filter-group">
            <label className="filter-label">Employee ID (Admin Account)</label>
            <input
              type="text"
              className="input"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="e.g. EMP1001 or EMP2001"
              required
            />
          </div>

          <div className="filter-group">
            <label className="filter-label">Password</label>
            <input
              type="password"
              className="input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter password"
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ height: 46, fontSize: '0.95rem', marginTop: 8 }}
            disabled={loading}
          >
            {loading ? (
              'Authenticating...'
            ) : (
              <>
                <LogIn size={18} /> Sign In as Admin
              </>
            )}
          </button>
        </form>

        <div style={{ marginTop: 24, paddingTop: 20, borderTop: '1px solid var(--border)' }}>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginBottom: 8, fontWeight: 600 }}>
            QUICK ACCESS ADMIN ACCOUNTS:
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleQuickFill('EMP1001')}
            >
              EMP1001
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleQuickFill('EMP2001')}
            >
              EMP2001
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
