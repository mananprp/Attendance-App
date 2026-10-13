import { ApiProperty } from '@nestjs/swagger';
import {
  IsString,
  IsNotEmpty,
  IsEnum,
  IsBoolean,
  IsNumber,
  IsOptional,
  IsUUID,
  Matches,
} from 'class-validator';
import { PunchType, TimeSource } from '../../../database/entities/attendance-punch.entity';

export class SyncPunchItemDto {
  @ApiProperty({ example: '550e8400-e29b-41d4-a716-446655440000' })
  @IsUUID()
  @IsNotEmpty()
  punch_uuid: string;

  @ApiProperty({ example: 'EMP1001' })
  @IsString()
  @IsNotEmpty()
  user_id: string;

  @ApiProperty({ example: '2026-09-30' })
  @IsString()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date_local: string;

  @ApiProperty({ example: 'IN', enum: ['IN', 'OUT'] })
  @IsEnum(PunchType)
  type: PunchType;

  @ApiProperty({ example: '2026-09-30T03:45:00.000Z' })
  @IsString()
  @IsNotEmpty()
  punch_time_utc: string;

  @ApiProperty({ example: 'NTP', enum: ['NTP', 'DEVICE'] })
  @IsEnum(TimeSource)
  time_source: TimeSource;

  @ApiProperty({ example: false })
  @IsBoolean()
  time_unverified: boolean;

  @ApiProperty({ example: 28.61394 })
  @IsNumber()
  latitude: number;

  @ApiProperty({ example: 77.209025 })
  @IsNumber()
  longitude: number;

  @ApiProperty({ example: 12.4 })
  @IsNumber()
  distance_m: number;

  @ApiProperty({ example: 0.8842 })
  @IsNumber()
  match_score: number;

  @ApiProperty({ example: '550e8400-e29b-41d4-a716-446655440000.jpg', required: false, nullable: true })
  @IsString()
  @IsOptional()
  image_file_name?: string | null;
}
