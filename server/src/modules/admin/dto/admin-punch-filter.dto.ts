import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString } from 'class-validator';

export class AdminPunchFilterDto {
  @ApiPropertyOptional({ example: '2026-10-01' })
  @IsOptional()
  @IsString()
  from_date?: string;

  @ApiPropertyOptional({ example: '2026-10-31' })
  @IsOptional()
  @IsString()
  to_date?: string;

  @ApiPropertyOptional({ example: 'EMP1001' })
  @IsOptional()
  @IsString()
  user_id?: string;

  @ApiPropertyOptional({ example: 'DEFAULT_OFFICE' })
  @IsOptional()
  @IsString()
  site_code?: string;

  @ApiPropertyOptional({ example: 'OFFICE', enum: ['OFFICE', 'NEW_LOCATION'] })
  @IsOptional()
  @IsString()
  location_type?: string;

  @ApiPropertyOptional({ example: 'GENERAL', enum: ['GENERAL', 'EVENING', 'NIGHT'] })
  @IsOptional()
  @IsString()
  shift_type?: string;

  @ApiPropertyOptional({ example: 'COMPLETE', enum: ['COMPLETE', 'IN_PROGRESS', 'MISSED_PUNCH'] })
  @IsOptional()
  @IsString()
  status?: string;
}
