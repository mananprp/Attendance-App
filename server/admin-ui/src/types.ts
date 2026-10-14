export interface AdminUser {
  user_id: string;
  name: string;
  role: 'ADMIN' | 'EMPLOYEE';
  site_code: string;
  site_name: string;
  status: string;
  photo_change_used: boolean;
}

export interface AdminSite {
  office_id: string;
  name: string;
  latitude: number;
  longitude: number;
  radius_m: number;
  timezone: string;
  match_threshold: number;
}

export interface AdminPunch {
  punch_uuid: string;
  user_id: string;
  user_name: string;
  site_code: string;
  date_local: string;
  type: 'IN' | 'OUT';
  location_type: 'OFFICE' | 'NEW_LOCATION';
  punch_time_utc: string;
  punch_time_local: string;
  time_source: 'NTP' | 'DEVICE';
  time_unverified: boolean;
  latitude: number;
  longitude: number;
  maps_url: string;
  distance_m: number;
  match_score: number;
}

export interface AdminDailyAttendance {
  user_id: string;
  user_name: string;
  site_code: string;
  date_local: string;
  shift_type: 'GENERAL' | 'EVENING' | 'NIGHT';
  in_time_local: string | null;
  in_time_utc: string | null;
  out_time_local: string | null;
  out_time_utc: string | null;
  work_duration_formatted: string;
  work_duration_minutes: number | null;
  location_type: 'OFFICE' | 'NEW_LOCATION';
  latitude: number | null;
  longitude: number | null;
  maps_url: string | null;
  distance_m: number | null;
  status: 'COMPLETE' | 'MISSED_PUNCH' | 'IN_PROGRESS';
  match_score_avg: number | null;
}

export interface AuthSession {
  token: string;
  user: {
    userId: string;
    name: string;
    role: string;
    siteCode?: string;
  };
}
