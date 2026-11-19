import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsString, IsNumber, IsOptional, Min, Max } from 'class-validator';

export class UpdateSiteDto {
  @ApiPropertyOptional({ example: 'Noida Branch (Renamed)', description: 'Display name of the office site' })
  @IsOptional()
  @IsString()
  name?: string;

  @ApiPropertyOptional({ example: 28.535516, description: 'Latitude coordinate of the site' })
  @IsOptional()
  @IsNumber()
  latitude?: number;

  @ApiPropertyOptional({ example: 77.391026, description: 'Longitude coordinate of the site' })
  @IsOptional()
  @IsNumber()
  longitude?: number;

  @ApiPropertyOptional({ example: 80, description: 'Geofence radius in meters' })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(10000)
  radius_m?: number;

  @ApiPropertyOptional({ example: 'Asia/Kolkata', description: 'Timezone of the site' })
  @IsOptional()
  @IsString()
  timezone?: string;

  @ApiPropertyOptional({ example: 0.46, description: 'Match threshold for face verification' })
  @IsOptional()
  @IsNumber()
  match_threshold?: number;
}
