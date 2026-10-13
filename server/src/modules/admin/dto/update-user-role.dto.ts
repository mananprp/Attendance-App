import { ApiProperty } from '@nestjs/swagger';
import { IsEnum } from 'class-validator';
import { UserRole } from '../../../database/entities/user.entity';

export class UpdateUserRoleDto {
  @ApiProperty({ example: 'ADMIN', enum: UserRole, description: 'Role to set' })
  @IsEnum(UserRole)
  role: UserRole;
}
