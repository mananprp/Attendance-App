import { AdminUser, AdminSite, AdminPunch, AdminDailyAttendance, AuthSession } from './types';

const API_BASE =
  import.meta.env.VITE_API_BASE ||
  (typeof window !== 'undefined' && !window.location.hostname.endsWith('vercel.app') && !window.location.hostname.endsWith('netlify.app')
    ? '/v1'
    : 'https://attendance.pess.co.in/v1');

export function getAuthToken(): string | null {
  return localStorage.getItem('admin_token');
}

export function getStoredUser(): AuthSession['user'] | null {
  const data = localStorage.getItem('admin_user');
  return data ? JSON.parse(data) : null;
}

export function saveSession(session: AuthSession) {
  localStorage.setItem('admin_token', session.token);
  localStorage.setItem('admin_user', JSON.stringify(session.user));
}

export function clearSession() {
  localStorage.removeItem('admin_token');
  localStorage.removeItem('admin_user');
}

let onUnauthorizedHandler: (() => void) | null = null;

export function setOnUnauthorized(callback: () => void) {
  onUnauthorizedHandler = callback;
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers = new Headers(options.headers || {});
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 || response.status === 403) {
    if (endpoint !== '/auth/login') {
      clearSession();
      if (onUnauthorizedHandler) {
        onUnauthorizedHandler();
      }
    }
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Request failed with status ${response.status}`);
  }

  return response.json();
}

export const api = {
  async login(userId: string, password: string): Promise<AuthSession> {
    const res = await request<any>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ user_id: userId, password }),
    });

    const token = res.token || res.access_token;
    if (!token) {
      throw new Error('Authentication failed: Server did not return an access token.');
    }

    const rawUser = res.user || {};
    if (rawUser.role !== 'ADMIN') {
      throw new Error('Access denied: Account does not have administrator privileges.');
    }

    const session: AuthSession = {
      token,
      user: {
        userId: rawUser.user_id || rawUser.userId,
        name: rawUser.display_name || rawUser.name || rawUser.user_id,
        role: rawUser.role,
        siteCode: rawUser.site_code || rawUser.siteCode,
      },
    };

    saveSession(session);
    return session;
  },

  async getPunches(filter: {
    from_date?: string;
    to_date?: string;
    user_id?: string;
    site_code?: string;
  } = {}): Promise<AdminPunch[]> {
    const params = new URLSearchParams();
    if (filter.from_date) params.append('from_date', filter.from_date);
    if (filter.to_date) params.append('to_date', filter.to_date);
    if (filter.user_id) params.append('user_id', filter.user_id);
    if (filter.site_code) params.append('site_code', filter.site_code);

    const query = params.toString() ? `?${params.toString()}` : '';
    return request<AdminPunch[]>(`/admin/punches${query}`);
  },

  async downloadPunchesCsv(filter: {
    from_date?: string;
    to_date?: string;
    user_id?: string;
    site_code?: string;
  } = {}): Promise<void> {
    const params = new URLSearchParams();
    if (filter.from_date) params.append('from_date', filter.from_date);
    if (filter.to_date) params.append('to_date', filter.to_date);
    if (filter.user_id) params.append('user_id', filter.user_id);
    if (filter.site_code) params.append('site_code', filter.site_code);

    const query = params.toString() ? `?${params.toString()}` : '';
    const token = getAuthToken();

    const response = await fetch(`${API_BASE}/admin/punches/export${query}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to export CSV');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance_punches_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  async getAttendance(filter: {
    from_date?: string;
    to_date?: string;
    user_id?: string;
    site_code?: string;
    location_type?: string;
    shift_type?: string;
    status?: string;
  } = {}): Promise<AdminDailyAttendance[]> {
    const params = new URLSearchParams();
    if (filter.from_date) params.append('from_date', filter.from_date);
    if (filter.to_date) params.append('to_date', filter.to_date);
    if (filter.user_id) params.append('user_id', filter.user_id);
    if (filter.site_code) params.append('site_code', filter.site_code);
    if (filter.location_type) params.append('location_type', filter.location_type);
    if (filter.shift_type) params.append('shift_type', filter.shift_type);
    if (filter.status) params.append('status', filter.status);

    const query = params.toString() ? `?${params.toString()}` : '';
    return request<AdminDailyAttendance[]>(`/admin/attendance${query}`);
  },

  async downloadAttendanceCsv(filter: {
    from_date?: string;
    to_date?: string;
    user_id?: string;
    site_code?: string;
    location_type?: string;
    shift_type?: string;
    status?: string;
  } = {}): Promise<void> {
    const params = new URLSearchParams();
    if (filter.from_date) params.append('from_date', filter.from_date);
    if (filter.to_date) params.append('to_date', filter.to_date);
    if (filter.user_id) params.append('user_id', filter.user_id);
    if (filter.site_code) params.append('site_code', filter.site_code);
    if (filter.location_type) params.append('location_type', filter.location_type);
    if (filter.shift_type) params.append('shift_type', filter.shift_type);
    if (filter.status) params.append('status', filter.status);

    const query = params.toString() ? `?${params.toString()}` : '';
    const token = getAuthToken();

    const response = await fetch(`${API_BASE}/admin/attendance/export${query}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Failed to export CSV');
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const userSuffix = filter.user_id ? `_${filter.user_id}` : '';
    a.download = `attendance${userSuffix}_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  async getUsers(): Promise<AdminUser[]> {
    return request<AdminUser[]>('/admin/users');
  },

  async createUser(data: {
    user_id: string;
    name: string;
    password: string;
    role?: 'EMPLOYEE' | 'ADMIN' | 'GUARD';
    site_code?: string;
  }): Promise<AdminUser> {
    return request<AdminUser>('/admin/users', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateUserSite(userId: string, siteCode: string): Promise<AdminUser> {
    return request<AdminUser>(`/admin/users/${userId}/site`, {
      method: 'PATCH',
      body: JSON.stringify({ site_code: siteCode }),
    });
  },

  async updateUserRole(userId: string, role: 'EMPLOYEE' | 'ADMIN'): Promise<AdminUser> {
    return request<AdminUser>(`/admin/users/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    });
  },

  async updateUserPassword(userId: string, password: string): Promise<AdminUser> {
    return request<AdminUser>(`/admin/users/${userId}/password`, {
      method: 'PATCH',
      body: JSON.stringify({ password }),
    });
  },

  async deleteUser(userId: string): Promise<{ success: boolean; message: string }> {
    return request<{ success: boolean; message: string }>(`/admin/users/${userId}`, {
      method: 'DELETE',
    });
  },

  async getSites(): Promise<AdminSite[]> {
    return request<AdminSite[]>('/admin/sites');
  },

  async createSite(data: {
    office_id: string;
    name: string;
    latitude: number;
    longitude: number;
    radius_m?: number;
    timezone?: string;
  }): Promise<AdminSite> {
    return request<AdminSite>('/admin/sites', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};
