import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class UpdateUserSiteDto {
  @ApiProperty({ example: 'SITE_REMOTE_001', description: 'Assigned office site ID' })
  @IsString()
  @IsNotEmpty()
  site_code: string;
}
