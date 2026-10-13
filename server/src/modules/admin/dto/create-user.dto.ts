import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsOptional, IsEnum, MinLength } from 'class-validator';
import { UserRole } from '../../../database/entities/user.entity';

export class CreateUserDto {
  @ApiProperty({ example: 'EMP1005', description: 'Unique alphanumeric employee ID' })
  @IsString()
  @IsNotEmpty()
  user_id: string;

  @ApiProperty({ example: 'Rahul Sharma', description: 'Full employee name' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: 'Password@123', description: 'Initial login password' })
  @IsString()
  @MinLength(6)
  password: string;

  @ApiPropertyOptional({ example: 'DEFAULT_OFFICE', description: 'Office site ID' })
  @IsString()
  @IsOptional()
  site_code?: string;

  @ApiPropertyOptional({ example: 'EMPLOYEE', enum: UserRole, description: 'Role' })
  @IsEnum(UserRole)
  @IsOptional()
  role?: UserRole;
}
