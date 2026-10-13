import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AttendancePunchEntity } from '../../database/entities/attendance-punch.entity';
import { AttendanceDayEntity } from '../../database/entities/attendance-day.entity';
import { SyncBatchEntity } from '../../database/entities/sync-batch.entity';
import { AttendanceController } from './attendance.controller';
import { AttendanceService } from './attendance.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      AttendancePunchEntity,
      AttendanceDayEntity,
      SyncBatchEntity,
    ]),
  ],
  controllers: [AttendanceController],
  providers: [AttendanceService],
  exports: [AttendanceService],
})
export class AttendanceModule {}
