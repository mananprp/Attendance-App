import { ApiProperty } from '@nestjs/swagger';

export class PhotoUploadResponseDto {
  @ApiProperty({ example: 1, enum: [1, 2], description: 'Photo version number' })
  version: number;

  @ApiProperty({
    example: '/uploads/photos/EMP1001_v1.jpg',
    description: 'URL path to download the uploaded photo',
  })
  photo_url: string;

  @ApiProperty({
    example: false,
    description: 'Whether one-time photo change has been consumed',
  })
  photo_change_used: boolean;
}

export class PhotoDetailResponseDto {
  @ApiProperty({ example: 1, enum: [1, 2] })
  version: number;

  @ApiProperty({ example: '/uploads/photos/EMP1001_v1.jpg' })
  photo_url: string;

  @ApiProperty({ example: '2026-09-30T09:00:00.000Z' })
  uploaded_at: string;

  @ApiProperty({
    example: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
  })
  hash: string;
}
