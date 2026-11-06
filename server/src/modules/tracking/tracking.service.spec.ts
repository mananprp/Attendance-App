import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { TrackingService } from './tracking.service';
import {
  TrackingSessionEntity,
  TrackingSessionStatus,
  TrackingPointEntity,
  TrackingStopEntity,
} from '../../database/entities';
import { ForbiddenException, NotFoundException } from '@nestjs/common';

describe('TrackingService', () => {
  let service: TrackingService;
  let sessionRepoMock: any;
  let pointRepoMock: any;
  let stopRepoMock: any;

  beforeEach(async () => {
    sessionRepoMock = {
      findOne: jest.fn(),
      create: jest.fn((dto) => ({ ...dto })),
      save: jest.fn((entity) => Promise.resolve(entity)),
      find: jest.fn(),
      createQueryBuilder: jest.fn(),
    };

    pointRepoMock = {
      create: jest.fn((dto) => ({ ...dto })),
      save: jest.fn((entities) => Promise.resolve(entities)),
      find: jest.fn(),
    };

    stopRepoMock = {
      create: jest.fn((dto) => ({ ...dto })),
      save: jest.fn((entities) => Promise.resolve(entities)),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TrackingService,
        {
          provide: getRepositoryToken(TrackingSessionEntity),
          useValue: sessionRepoMock,
        },
        {
          provide: getRepositoryToken(TrackingPointEntity),
          useValue: pointRepoMock,
        },
        {
          provide: getRepositoryToken(TrackingStopEntity),
          useValue: stopRepoMock,
        },
      ],
    }).compile();

    service = module.get<TrackingService>(TrackingService);
  });

  describe('startSession', () => {
    it('creates and saves a new active tracking session', async () => {
      sessionRepoMock.findOne.mockResolvedValue(null);

      const result = await service.startSession('USER1', {
        sessionId: 'sess-123',
        startTimeUtc: 1700000000000,
      });

      expect(sessionRepoMock.create).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'sess-123',
          userId: 'USER1',
          status: TrackingSessionStatus.ACTIVE,
        }),
      );
      expect(sessionRepoMock.save).toHaveBeenCalled();
      expect(result.success).toBe(true);
      expect(result.sessionId).toBe('sess-123');
    });
  });

  describe('finishSession', () => {
    it('completes session, updates distance, saves points and stops', async () => {
      const existingSession = {
        id: 'sess-123',
        userId: 'USER1',
        startTimeUtc: new Date(1700000000000),
        status: TrackingSessionStatus.ACTIVE,
        totalDistanceM: 0,
        stopsCount: 0,
      };
      sessionRepoMock.findOne.mockResolvedValue(existingSession);

      const result = await service.finishSession('USER1', 'sess-123', {
        endTimeUtc: 1700003600000,
        totalDistanceM: 5200.5,
        points: [
          {
            id: 'pt-1',
            latitude: 28.61,
            longitude: 77.2,
            accuracyM: 5,
            recordedAtUtc: 1700000060000,
            isPausedGap: false,
          },
        ],
        stops: [
          {
            id: 'stop-1',
            latitude: 28.61,
            longitude: 77.2,
            stopName: 'Site A Inspection',
            arrivedAtUtc: 1700001000000,
          },
        ],
      });

      expect(sessionRepoMock.save).toHaveBeenCalled();
      expect(existingSession.status).toBe(TrackingSessionStatus.COMPLETED);
      expect(existingSession.totalDistanceM).toBe(5200.5);
      expect(pointRepoMock.save).toHaveBeenCalled();
      expect(stopRepoMock.save).toHaveBeenCalled();
      expect(result.success).toBe(true);
      expect(result.totalPointsSaved).toBe(1);
      expect(result.stopsCount).toBe(1);
    });

    it('throws ForbiddenException if user tries to finish another user session', async () => {
      sessionRepoMock.findOne.mockResolvedValue({
        id: 'sess-123',
        userId: 'DIFFERENT_USER',
      });

      await expect(
        service.finishSession('USER1', 'sess-123', {
          endTimeUtc: 1700003600000,
          totalDistanceM: 0,
          points: [],
          stops: [],
        }),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  describe('getSessionDetails', () => {
    it('returns session with points and stops', async () => {
      const mockSession = {
        id: 'sess-123',
        userId: 'USER1',
        stops: [{ id: 'stop-1', stopName: 'Site 1' }],
      };
      sessionRepoMock.findOne.mockResolvedValue(mockSession);
      pointRepoMock.find.mockResolvedValue([{ id: 'pt-1' }]);

      const result = await service.getSessionDetails('USER1', 'sess-123');
      expect(result.id).toBe('sess-123');
      expect(result.points.length).toBe(1);
      expect(result.stops.length).toBe(1);
    });

    it('throws NotFoundException if session does not exist', async () => {
      sessionRepoMock.findOne.mockResolvedValue(null);

      await expect(
        service.getSessionDetails('USER1', 'nonexistent'),
      ).rejects.toThrow(NotFoundException);
    });
  });
});
