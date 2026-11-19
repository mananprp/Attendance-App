import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, IsEnum, IsArray } from 'class-validator';
import { UserRole } from '../../../database/entities/user.entity';

export class UpdateUserDto {
  @ApiPropertyOptional({ example: 'Rahul Sharma', description: 'Updated full employee name' })
  @IsString()
  @IsOptional()
  name?: string;

  @ApiPropertyOptional({ example: 'EMPLOYEE', enum: UserRole, description: 'Updated role' })
  @IsEnum(UserRole)
  @IsOptional()
  role?: UserRole;

  @ApiPropertyOptional({ example: 'DEFAULT_OFFICE', description: 'Primary office site ID' })
  @IsString()
  @IsOptional()
  site_code?: string;

  @ApiPropertyOptional({ example: ['DEFAULT_OFFICE'], description: 'Assigned office site IDs' })
  @IsArray()
  @IsOptional()
  site_codes?: string[];
}
