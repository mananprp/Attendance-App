import { ApiProperty } from '@nestjs/swagger';

export class AdminUserDto {
  @ApiProperty({ example: 'EMP1001' })
  user_id: string;

  @ApiProperty({ example: 'Rahul Sharma' })
  name: string;

  @ApiProperty({ example: 'EMPLOYEE', enum: ['ADMIN', 'SUPERVISOR', 'EMPLOYEE'] })
  role: string;

  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  site_code: string;

  @ApiProperty({ example: 'Headquarters' })
  site_name: string;

  @ApiProperty({ example: [{ site_code: 'DEFAULT_OFFICE', site_name: 'Headquarters' }], required: false })
  assigned_sites?: { site_code: string; site_name: string }[];

  @ApiProperty({ example: 'ACTIVE' })
  status: string;

  @ApiProperty({ example: false })
  photo_change_used: boolean;
}
