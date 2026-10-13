import { Test, TestingModule } from '@nestjs/testing';
import { AttendanceController } from './attendance.controller';
import { AttendanceService } from './attendance.service';
import { PunchSyncStatus } from './dto/sync-batch-response.dto';
import { AttendanceDayStatus } from '../../database/entities/attendance-day.entity';

describe('AttendanceController', () => {
  let controller: AttendanceController;
  let service: any;

  const mockUser = { userId: 'EMP1001', name: 'Alice Sharma' };

  beforeEach(async () => {
    service = {
      syncPunches: jest.fn().mockResolvedValue({
        batch_id: 'mock-batch-id',
        processed_count: 1,
        results: [
          {
            punch_uuid: 'punch-123',
            status: PunchSyncStatus.ACCEPTED,
            reason: null,
          },
        ],
      }),
      getMonthlyAttendance: jest.fn().mockResolvedValue({
        month: '2026-09',
        days: [
          {
            date_local: '2026-09-30',
            in_time: '2026-09-30T03:45:00.000Z',
            out_time: '2026-09-30T12:30:00.000Z',
            status: AttendanceDayStatus.COMPLETE,
            time_unverified: false,
          },
        ],
        punches: [],
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [AttendanceController],
      providers: [{ provide: AttendanceService, useValue: service }],
    }).compile();

    controller = module.get<AttendanceController>(AttendanceController);
  });

  it('should handle punch sync', async () => {
    const result = await controller.syncPunches(mockUser, '[]', []);
    expect(result.batch_id).toBe('mock-batch-id');
    expect(service.syncPunches).toHaveBeenCalledWith('EMP1001', '[]', []);
  });

  it('should fetch monthly attendance', async () => {
    const result = await controller.getMonthlyAttendance(mockUser, '2026-09');
    expect(result.month).toBe('2026-09');
    expect(result.days.length).toBe(1);
    expect(service.getMonthlyAttendance).toHaveBeenCalledWith('EMP1001', '2026-09');
  });
});
