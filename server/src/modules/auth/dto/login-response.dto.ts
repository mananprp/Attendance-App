import { ApiProperty } from '@nestjs/swagger';

export class UserProfileDto {
  @ApiProperty({ example: 'EMP1001' })
  user_id: string;

  @ApiProperty({ example: 'Alice Sharma' })
  display_name: string;

  @ApiProperty({ example: 'EMPLOYEE', enum: ['ADMIN', 'EMPLOYEE'] })
  role: string;

  @ApiProperty({ example: 'DEFAULT_OFFICE' })
  site_code: string;

  @ApiProperty({ example: false })
  photo_change_used: boolean;

  @ApiProperty({ example: true })
  has_photo: boolean;

  @ApiProperty({ example: 1, nullable: true })
  active_photo_version: number | null;

  @ApiProperty({
    example: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    nullable: true,
  })
  active_photo_hash: string | null;
}

export class LoginResponseDto {
  @ApiProperty({ example: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' })
  token: string;

  @ApiProperty({ type: UserProfileDto })
  user: UserProfileDto;
}
