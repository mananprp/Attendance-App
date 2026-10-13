import { ApiProperty } from '@nestjs/swagger';

export class AdminSiteDto {
  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  office_id: string;

  @ApiProperty({ example: 'Headquarters' })
  name: string;

  @ApiProperty({ example: 28.490293 })
  latitude: number;

  @ApiProperty({ example: 77.060544 })
  longitude: number;

  @ApiProperty({ example: 100 })
  radius_m: number;

  @ApiProperty({ example: 'Asia/Kolkata' })
  timezone: string;

  @ApiProperty({ example: 0.46 })
  match_threshold: number;
}
