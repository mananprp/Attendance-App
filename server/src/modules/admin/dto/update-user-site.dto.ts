import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, IsArray } from 'class-validator';

export class UpdateUserSiteDto {
  @ApiPropertyOptional({ example: 'SITE_REMOTE_001', description: 'Assigned office site ID' })
  @IsString()
  @IsOptional()
  site_code?: string;

  @ApiPropertyOptional({ example: ['SITE_REMOTE_001', 'DEFAULT_OFFICE'], description: 'Assigned office site IDs' })
  @IsArray()
  @IsOptional()
  site_codes?: string[];
}
