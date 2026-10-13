import { ApiProperty } from '@nestjs/swagger';
import { SyncPunchItemDto } from './sync-punch-item.dto';
import { AttendanceDayStatus } from '../../../database/entities/attendance-day.entity';

export class DaySummaryDto {
  @ApiProperty({ example: '2026-09-30' })
  date_local: string;

  @ApiProperty({ example: '2026-09-30T03:45:00.000Z', nullable: true })
  in_time: string | null;

  @ApiProperty({ example: '2026-09-30T12:30:00.000Z', nullable: true })
  out_time: string | null;

  @ApiProperty({
    example: 'COMPLETE',
    enum: ['COMPLETE', 'MISSED_PUNCH', 'NO_RECORD'],
  })
  status: AttendanceDayStatus;

  @ApiProperty({ example: false })
  time_unverified: boolean;
}

export class MonthlyAttendanceResponseDto {
  @ApiProperty({ example: '2026-09' })
  month: string;

  @ApiProperty({ type: [DaySummaryDto] })
  days: DaySummaryDto[];

  @ApiProperty({ type: [SyncPunchItemDto] })
  punches: SyncPunchItemDto[];
}
