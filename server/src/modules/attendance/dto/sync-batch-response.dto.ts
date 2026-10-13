import { ApiProperty } from '@nestjs/swagger';

export enum PunchSyncStatus {
  ACCEPTED = 'ACCEPTED',
  DUPLICATE = 'DUPLICATE',
  REJECTED = 'REJECTED',
}

export class PunchSyncResultDto {
  @ApiProperty({ example: '550e8400-e29b-41d4-a716-446655440000' })
  punch_uuid: string;

  @ApiProperty({ example: 'ACCEPTED', enum: PunchSyncStatus })
  status: PunchSyncStatus;

  @ApiProperty({ example: null, nullable: true })
  reason: string | null;
}

export class SyncBatchResponseDto {
  @ApiProperty({ example: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d' })
  batch_id: string;

  @ApiProperty({ example: 2 })
  processed_count: number;

  @ApiProperty({ type: [PunchSyncResultDto] })
  results: PunchSyncResultDto[];
}
