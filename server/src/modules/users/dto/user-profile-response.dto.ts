import { ApiProperty } from '@nestjs/swagger';

export class AssignedSiteSummaryDto {
  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  site_code: string;

  @ApiProperty({ example: 'Headquarters' })
  site_name: string;
}

export class UserProfileResponseDto {
  @ApiProperty({ example: 'EMP1001' })
  user_id: string;

  @ApiProperty({ example: 'Alice Sharma' })
  name: string;

  @ApiProperty({ example: 'EMPLOYEE' })
  role: string;

  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  site_code: string;

  @ApiProperty({ example: 'Headquarters' })
  site_name: string;

  @ApiProperty({ type: [AssignedSiteSummaryDto] })
  assigned_sites: AssignedSiteSummaryDto[];

  @ApiProperty({ example: true })
  has_photo: boolean;

  @ApiProperty({ example: 1, required: false, nullable: true })
  photo_version?: number;

  @ApiProperty({ example: 'a1b2c3d4...', required: false, nullable: true })
  photo_hash?: string;

  @ApiProperty({ example: false })
  photo_change_used: boolean;
}
