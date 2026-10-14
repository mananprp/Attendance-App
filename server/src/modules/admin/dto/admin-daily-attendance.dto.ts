import { ApiProperty } from '@nestjs/swagger';
import { LocationType } from '../../../database/entities/attendance-punch.entity';

export class AdminDailyAttendanceDto {
  @ApiProperty({ example: 'EMP1001' })
  user_id: string;

  @ApiProperty({ example: 'Alice Sharma' })
  user_name: string;

  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  site_code: string;

  @ApiProperty({ example: '2026-10-14' })
  date_local: string;

  @ApiProperty({ example: 'GENERAL', enum: ['GENERAL', 'EVENING', 'NIGHT'] })
  shift_type: string;

  @ApiProperty({ example: '09:15:20 AM', nullable: true })
  in_time_local: string | null;

  @ApiProperty({ example: '2026-10-14T03:45:20.000Z', nullable: true })
  in_time_utc: string | null;

  @ApiProperty({ example: '06:30:15 PM', nullable: true })
  out_time_local: string | null;

  @ApiProperty({ example: '2026-10-14T13:00:15.000Z', nullable: true })
  out_time_utc: string | null;

  @ApiProperty({ example: '9h 15m' })
  work_duration_formatted: string;

  @ApiProperty({ example: 555, nullable: true })
  work_duration_minutes: number | null;

  @ApiProperty({ enum: LocationType })
  location_type: LocationType;

  @ApiProperty({ example: 28.61394, nullable: true })
  latitude: number | null;

  @ApiProperty({ example: 77.20902, nullable: true })
  longitude: number | null;

  @ApiProperty({ example: 'https://www.google.com/maps/search/?api=1&query=28.61394,77.20902', nullable: true })
  maps_url: string | null;

  @ApiProperty({ example: 0, nullable: true })
  distance_m: number | null;

  @ApiProperty({ example: 'COMPLETE', enum: ['COMPLETE', 'MISSED_PUNCH', 'IN_PROGRESS'] })
  status: string;

  @ApiProperty({ example: 0.95, nullable: true })
  match_score_avg: number | null;
}
