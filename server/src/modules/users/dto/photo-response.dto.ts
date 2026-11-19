import { ApiProperty } from '@nestjs/swagger';

export class PhotoUploadResponseDto {
  @ApiProperty({ example: 1, enum: [1, 2], description: 'Photo version number' })
  version: number;

  @ApiProperty({
    example: '/v1/users/me/photo/file',
    description: 'URL path to download the uploaded photo',
  })
  photo_url: string;

  @ApiProperty({
    example: false,
    description: 'Whether one-time photo change has been consumed',
  })
  photo_change_used: boolean;

  @ApiProperty({
    example: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    description: 'SHA-256 hash of the uploaded photo file',
  })
  hash: string;
}

export class PhotoDetailResponseDto {
  @ApiProperty({ example: 'EMP1001', description: 'Owner user ID' })
  user_id: string;

  @ApiProperty({ example: 1, enum: [1, 2] })
  version: number;

  @ApiProperty({ example: '/v1/users/me/photo/file' })
  photo_url: string;

  @ApiProperty({ example: '2026-09-30T09:00:00.000Z' })
  uploaded_at: string;

  @ApiProperty({
    example: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
  })
  hash: string;
}
