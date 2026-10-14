import React, { useState, useEffect } from 'react';
import { api, getStoredUser, getAuthToken, setOnUnauthorized } from './api';
import { AuthSession, AdminSite } from './types';
import { LoginPage } from './components/LoginPage';
import { Navbar } from './components/Navbar';
import { PunchesView } from './components/PunchesView';
import { UsersView } from './components/UsersView';
import { SitesView } from './components/SitesView';

export function App() {
  const [session, setSession] = useState<AuthSession | null>(() => {
    const token = getAuthToken();
    const user = getStoredUser();
    return token && user ? { token, user } : null;
  });

  const [activeTab, setActiveTab] = useState<'punches' | 'users' | 'sites'>('punches');
  const [sites, setSites] = useState<AdminSite[]>([]);

  useEffect(() => {
    setOnUnauthorized(() => {
      setSession(null);
    });
  }, []);

  const loadSites = async () => {
    try {
      const data = await api.getSites();
      setSites(data);
    } catch (err) {
      console.error('Failed to load sites', err);
    }
  };

  useEffect(() => {
    if (session) {
      loadSites();
    }
  }, [session]);

  if (!session) {
    return <LoginPage onLoginSuccess={(newSession) => setSession(newSession)} />;
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar
        session={session}
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        onLogout={() => setSession(null)}
      />

      <main className="main-content" style={{ flex: 1 }}>
        {activeTab === 'punches' && <PunchesView sites={sites} />}
        {activeTab === 'users' && <UsersView sites={sites} />}
        {activeTab === 'sites' && <SitesView sites={sites} onRefreshSites={loadSites} />}
      </main>
    </div>
  );
}

export default App;
