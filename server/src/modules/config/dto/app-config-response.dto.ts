import { ApiProperty } from '@nestjs/swagger';

export class AppConfigResponseDto {
  @ApiProperty({ example: 28.613939, description: 'Office latitude' })
  office_lat: number;

  @ApiProperty({ example: 77.209021, description: 'Office longitude' })
  office_lng: number;

  @ApiProperty({ example: 100, description: 'Geofence radius in meters' })
  radius_m: number;

  @ApiProperty({ example: 'Asia/Kolkata', description: 'Office timezone' })
  timezone: string;

  @ApiProperty({
    example: ['16:00', '00:00'],
    description: 'Scheduled sync time windows (HH:mm)',
    type: [String],
  })
  sync_windows: string[];

  @ApiProperty({
    example: 30,
    description: 'Random jitter window before sync in minutes',
  })
  sync_jitter_minutes: number;

  @ApiProperty({
    example: 0.55,
    description: 'Minimum cosine similarity threshold for face match',
  })
  match_threshold: number;
}
