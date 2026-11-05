import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { BadRequestException } from '@nestjs/common';
import { AttendanceService } from './attendance.service';
import {
  AttendancePunchEntity,
  PunchType,
  TimeSource,
} from '../../database/entities/attendance-punch.entity';
import {
  AttendanceDayEntity,
  AttendanceDayStatus,
} from '../../database/entities/attendance-day.entity';
import { SyncBatchEntity } from '../../database/entities/sync-batch.entity';
import { PunchSyncStatus } from './dto/sync-batch-response.dto';

describe('AttendanceService', () => {
  let service: AttendanceService;
  let punchRepo: any;
  let dayRepo: any;
  let syncBatchRepo: any;

  beforeEach(async () => {
    punchRepo = {
      findOne: jest.fn(),
      find: jest.fn(),
      create: jest.fn((dto) => dto),
      save: jest.fn((entity) => Promise.resolve(entity)),
    };
    dayRepo = {
      findOne: jest.fn(),
      find: jest.fn(),
      create: jest.fn((dto) => dto),
      save: jest.fn((entity) => Promise.resolve(entity)),
    };
    syncBatchRepo = {
      create: jest.fn((dto) => dto),
      save: jest.fn((entity) => Promise.resolve(entity)),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AttendanceService,
        {
          provide: getRepositoryToken(AttendancePunchEntity),
          useValue: punchRepo,
        },
        {
          provide: getRepositoryToken(AttendanceDayEntity),
          useValue: dayRepo,
        },
        {
          provide: getRepositoryToken(SyncBatchEntity),
          useValue: syncBatchRepo,
        },
      ],
    }).compile();

    service = module.get<AttendanceService>(AttendanceService);
  });

  describe('syncPunches', () => {
    it('should ignore attached multipart image files and store image_path as null for punches', async () => {
      punchRepo.findOne.mockResolvedValue(null);
      punchRepo.find.mockResolvedValue([
        { type: PunchType.IN, punchTimeUtc: new Date('2026-09-30T03:45:00.000Z'), timeUnverified: false },
      ]);
      dayRepo.findOne.mockResolvedValue(null);

      const punchWithImg = {
        ...punchIn,
        image_file_name: 'test_punch.jpg',
      };
      const batchJson = JSON.stringify([punchWithImg]);
      const mockFiles = [
        {
          fieldname: 'files',
          originalname: 'test_punch.jpg',
          buffer: Buffer.from('punch_image_bytes'),
        } as Express.Multer.File,
      ];

      const result = await service.syncPunches('EMP1001', batchJson, mockFiles);
      expect(result.results[0].status).toBe(PunchSyncStatus.ACCEPTED);
      expect(punchRepo.create).toHaveBeenCalledWith(
        expect.objectContaining({
          imagePath: null,
        }),
      );
    });

    const punchIn = {
      punch_uuid: '550e8400-e29b-41d4-a716-446655440001',
      user_id: 'EMP1001',
      date_local: '2026-09-30',
      type: 'IN',
      punch_time_utc: '2026-09-30T03:45:00.000Z',
      time_source: 'NTP',
      time_unverified: false,
      latitude: 28.61394,
      longitude: 77.209025,
      distance_m: 12.4,
      match_score: 0.8842,
    };

    const punchOut = {
      punch_uuid: '550e8400-e29b-41d4-a716-446655440002',
      user_id: 'EMP1001',
      date_local: '2026-09-30',
      type: 'OUT',
      punch_time_utc: '2026-09-30T12:30:00.000Z',
      time_source: 'NTP',
      time_unverified: false,
      latitude: 28.61394,
      longitude: 77.209025,
      distance_m: 14.1,
      match_score: 0.9125,
    };

    it('should accept new punches and update daily summary to COMPLETE', async () => {
      punchRepo.findOne.mockResolvedValue(null);
      punchRepo.find.mockResolvedValue([
        { type: PunchType.IN, punchTimeUtc: new Date('2026-09-30T03:45:00.000Z'), timeUnverified: false },
        { type: PunchType.OUT, punchTimeUtc: new Date('2026-09-30T12:30:00.000Z'), timeUnverified: false },
      ]);
      dayRepo.findOne.mockResolvedValue(null);

      const batchJson = JSON.stringify([punchIn, punchOut]);
      const result = await service.syncPunches('EMP1001', batchJson, []);

      expect(result.processed_count).toBe(2);
      expect(result.results[0].status).toBe(PunchSyncStatus.ACCEPTED);
      expect(result.results[1].status).toBe(PunchSyncStatus.ACCEPTED);
      expect(dayRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({
          userId: 'EMP1001',
          date: '2026-09-30',
          status: AttendanceDayStatus.COMPLETE,
          timeUnverified: false,
        }),
      );
    });

    it('should return DUPLICATE for already synced punches without failing the batch', async () => {
      punchRepo.findOne.mockResolvedValue({ punchUuid: punchIn.punch_uuid });
      const batchJson = JSON.stringify([punchIn]);

      const result = await service.syncPunches('EMP1001', batchJson, []);
      expect(result.results[0].status).toBe(PunchSyncStatus.DUPLICATE);
      expect(result.results[0].reason).toContain('already synced');
    });

    it('should mark day status MISSED_PUNCH if only IN punch exists', async () => {
      punchRepo.findOne.mockResolvedValue(null);
      punchRepo.find.mockResolvedValue([
        { type: PunchType.IN, punchTimeUtc: new Date('2026-09-30T03:45:00.000Z'), timeUnverified: false },
      ]);
      dayRepo.findOne.mockResolvedValue(null);

      const batchJson = JSON.stringify([punchIn]);
      await service.syncPunches('EMP1001', batchJson, []);

      expect(dayRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({
          status: AttendanceDayStatus.MISSED_PUNCH,
        }),
      );
    });

    it('should throw BadRequestException on invalid batch JSON', async () => {
      await expect(
        service.syncPunches('EMP1001', 'not-valid-json', []),
      ).rejects.toThrow(BadRequestException);
    });
  });

  describe('getMonthlyAttendance', () => {
    it('should return monthly attendance days and punches', async () => {
      dayRepo.find.mockResolvedValue([
        {
          date: '2026-09-30',
          inTime: new Date('2026-09-30T03:45:00.000Z'),
          outTime: new Date('2026-09-30T12:30:00.000Z'),
          status: AttendanceDayStatus.COMPLETE,
          timeUnverified: false,
        },
      ]);
      punchRepo.find.mockResolvedValue([
        {
          punchUuid: '550e8400-e29b-41d4-a716-446655440001',
          userId: 'EMP1001',
          dateLocal: '2026-09-30',
          type: PunchType.IN,
          punchTimeUtc: new Date('2026-09-30T03:45:00.000Z'),
          timeSource: TimeSource.NTP,
          timeUnverified: false,
          latitude: 28.61394,
          longitude: 77.209025,
          distanceM: 12.4,
          matchScore: 0.8842,
          imagePath: '/uploads/punches/punch1.jpg',
        },
      ]);

      const result = await service.getMonthlyAttendance('EMP1001', '2026-09');
      expect(result.month).toBe('2026-09');
      expect(result.days.length).toBe(1);
      expect(result.days[0].status).toBe(AttendanceDayStatus.COMPLETE);
      expect(result.punches.length).toBe(1);
      expect(result.punches[0].image_file_name).toBe('punch1.jpg');
    });

    it('should throw BadRequestException if month format is invalid', async () => {
      await expect(
        service.getMonthlyAttendance('EMP1001', '2026-13'),
      ).rejects.toThrow(BadRequestException);

      await expect(
        service.getMonthlyAttendance('EMP1001', 'invalid-month'),
      ).rejects.toThrow(BadRequestException);
    });
  });
});
