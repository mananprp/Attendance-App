import { ApiProperty } from '@nestjs/swagger';

export class ModelManifestItemDto {
  @ApiProperty({ example: 'det_10g' })
  name: string;

  @ApiProperty({ example: 'det_10g.onnx' })
  file_name: string;

  @ApiProperty({ example: 17825792 })
  size_bytes: number;

  @ApiProperty({
    example: '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
  })
  sha256: string;

  @ApiProperty({
    example: 'https://models.attendance.company.com/det_10g.onnx',
  })
  download_url: string;
}

export class ModelManifestResponseDto {
  @ApiProperty({ type: [ModelManifestItemDto] })
  models: ModelManifestItemDto[];

  @ApiProperty({ example: 193303552 })
  total_size_bytes: number;
}
