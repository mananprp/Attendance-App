import { ApiProperty } from '@nestjs/swagger';
import { LocationType, PunchType, TimeSource } from '../../../database/entities/attendance-punch.entity';

export class AdminPunchDto {
  @ApiProperty()
  punch_uuid: string;

  @ApiProperty()
  user_id: string;

  @ApiProperty()
  user_name: string;

  @ApiProperty()
  site_code: string;

  @ApiProperty()
  date_local: string;

  @ApiProperty({ enum: PunchType })
  type: PunchType;

  @ApiProperty({ enum: LocationType })
  location_type: LocationType;

  @ApiProperty()
  punch_time_utc: string;

  @ApiProperty()
  punch_time_local: string;

  @ApiProperty({ enum: TimeSource })
  time_source: TimeSource;

  @ApiProperty()
  time_unverified: boolean;

  @ApiProperty()
  latitude: number;

  @ApiProperty()
  longitude: number;

  @ApiProperty()
  maps_url: string;

  @ApiProperty()
  distance_m: number;

  @ApiProperty()
  match_score: number;
}
