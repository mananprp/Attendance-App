import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsNumber, IsOptional, Min, Max } from 'class-validator';

export class CreateSiteDto {
  @ApiProperty({ example: 'REMOTE_SITE_02', description: 'Unique identifier / code for the office site' })
  @IsString()
  @IsNotEmpty()
  office_id: string;

  @ApiProperty({ example: 'Noida Branch', description: 'Display name of the office site' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: 28.535516, description: 'Latitude coordinate of the site' })
  @IsNumber()
  @IsNotEmpty()
  latitude: number;

  @ApiProperty({ example: 77.391026, description: 'Longitude coordinate of the site' })
  @IsNumber()
  @IsNotEmpty()
  longitude: number;

  @ApiPropertyOptional({ example: 100, description: 'Geofence radius in meters', default: 100 })
  @IsOptional()
  @IsNumber()
  @Min(10)
  @Max(10000)
  radius_m?: number;

  @ApiPropertyOptional({ example: 'Asia/Kolkata', description: 'Timezone of the site', default: 'Asia/Kolkata' })
  @IsOptional()
  @IsString()
  timezone?: string;

  @ApiPropertyOptional({ example: 0.46, description: 'Match threshold for face verification', default: 0.46 })
  @IsOptional()
  @IsNumber()
  match_threshold?: number;
}
