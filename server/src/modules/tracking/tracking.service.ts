import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import {
  TrackingSessionEntity,
  TrackingSessionStatus,
  TrackingPointEntity,
  TrackingStopEntity,
  UserEntity,
  UserSiteEntity,
  OfficeConfigEntity,
  UserRole,
} from '../../database/entities';
import { StartTrackingSessionDto } from './dto/start-tracking-session.dto';
import { FinishTrackingSessionDto } from './dto/finish-tracking-session.dto';
import { AuthenticatedUser } from '../../common/decorators/current-user.decorator';

@Injectable()
export class TrackingService {
  constructor(
    @InjectRepository(TrackingSessionEntity)
    private readonly sessionRepo: Repository<TrackingSessionEntity>,
    @InjectRepository(TrackingPointEntity)
    private readonly pointRepo: Repository<TrackingPointEntity>,
    @InjectRepository(TrackingStopEntity)
    private readonly stopRepo: Repository<TrackingStopEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepo: Repository<UserEntity>,
    @InjectRepository(UserSiteEntity)
    private readonly userSiteRepo: Repository<UserSiteEntity>,
    @InjectRepository(OfficeConfigEntity)
    private readonly officeConfigRepo: Repository<OfficeConfigEntity>,
  ) {}

  async startSession(userId: string, dto: StartTrackingSessionDto) {
    let session = await this.sessionRepo.findOne({ where: { id: dto.sessionId } });
    if (!session) {
      session = this.sessionRepo.create({
        id: dto.sessionId,
        userId,
        startTimeUtc: new Date(dto.startTimeUtc),
        status: TrackingSessionStatus.ACTIVE,
        totalDistanceM: 0,
        stopsCount: 0,
      });
      await this.sessionRepo.save(session);
    }
    return {
      success: true,
      sessionId: session.id,
      status: session.status,
      startTimeUtc: session.startTimeUtc,
    };
  }

  async finishSession(
    userId: string,
    sessionId: string,
    dto: FinishTrackingSessionDto,
  ) {
    let session = await this.sessionRepo.findOne({ where: { id: sessionId } });
    if (!session) {
      session = this.sessionRepo.create({
        id: sessionId,
        userId,
        startTimeUtc: new Date(
          dto.points.length > 0 ? dto.points[0].recordedAtUtc : Date.now(),
        ),
      });
    }

    if (session.userId !== userId) {
      throw new ForbiddenException('Cannot finish a tracking session of another user');
    }

    session.endTimeUtc = new Date(dto.endTimeUtc);
    session.totalDistanceM = dto.totalDistanceM;
    session.status = TrackingSessionStatus.COMPLETED;
    session.stopsCount = dto.stops.length;

    await this.sessionRepo.save(session);

    // Save points in chunks if large
    if (dto.points && dto.points.length > 0) {
      const pointsToSave = dto.points.map((p) =>
        this.pointRepo.create({
          id: p.id,
          sessionId,
          latitude: p.latitude,
          longitude: p.longitude,
          accuracyM: p.accuracyM ?? null,
          recordedAtUtc: new Date(p.recordedAtUtc),
          isPausedGap: p.isPausedGap ?? false,
        }),
      );

      // Save in batches of 500
      const batchSize = 500;
      for (let i = 0; i < pointsToSave.length; i += batchSize) {
        await this.pointRepo.save(pointsToSave.slice(i, i + batchSize));
      }
    }

    // Save stops
    if (dto.stops && dto.stops.length > 0) {
      const stopsToSave = dto.stops.map((s) =>
        this.stopRepo.create({
          id: s.id,
          sessionId,
          latitude: s.latitude,
          longitude: s.longitude,
          stopName: s.stopName ?? null,
          arrivedAtUtc: new Date(s.arrivedAtUtc),
        }),
      );
      await this.stopRepo.save(stopsToSave);
    }

    return {
      success: true,
      sessionId,
      totalDistanceM: session.totalDistanceM,
      stopsCount: session.stopsCount,
      totalPointsSaved: dto.points.length,
    };
  }

  async getUserSessions(userId: string) {
    return this.sessionRepo.find({
      where: { userId },
      order: { startTimeUtc: 'DESC' },
      take: 50,
    });
  }

  async getSessionDetails(userId: string, sessionId: string, isAdmin = false) {
    const session = await this.sessionRepo.findOne({
      where: { id: sessionId },
      relations: ['user', 'stops'],
    });

    if (!session) {
      throw new NotFoundException(`Tracking session ${sessionId} not found`);
    }

    if (!isAdmin && session.userId !== userId) {
      throw new ForbiddenException('Access denied to this tracking session');
    }

    const points = await this.pointRepo.find({
      where: { sessionId },
      order: { recordedAtUtc: 'ASC' },
    });

    return {
      ...session,
      points,
    };
  }

  private async autoCloseExpiredSessions(): Promise<void> {
    try {
      const activeSessions = await this.sessionRepo.find({
        where: { status: TrackingSessionStatus.ACTIVE },
      });
      const now = new Date();

      for (const s of activeSessions) {
        const start = new Date(s.startTimeUtc);
        // Next midnight in IST (UTC+5.5)
        const istStartDay = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(start);
        const istToday = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(now);

        if (istStartDay < istToday) {
          // It started on a prior day and is still ACTIVE -> cutoff at midnight of that day
          const [y, m, d] = istStartDay.split('-').map(Number);
          const midnightUtc = new Date(Date.UTC(y, m - 1, d + 1, 0, 0, 0) - (5.5 * 3600 * 1000));
          s.status = TrackingSessionStatus.COMPLETED;
          s.endTimeUtc = midnightUtc;
          await this.sessionRepo.save(s);
        }
      }
    } catch (e) {
      // Non-blocking background catch
    }
  }

  async getAdminSessions(caller?: AuthenticatedUser, userId?: string, limit = 50) {
    await this.autoCloseExpiredSessions();

    const qb = this.sessionRepo
      .createQueryBuilder('session')
      .leftJoinAndSelect('session.user', 'user')
      .orderBy('session.startTimeUtc', 'DESC')
      .take(limit);

    if (caller && caller.role === UserRole.SUPERVISOR) {
      const userSites = await this.userSiteRepo.find({ where: { userId: caller.userId } });
      const assignedCodes = new Set(userSites.map((us) => us.siteCode));
      if (caller.siteCode) assignedCodes.add(caller.siteCode);

      const createdSites = await this.officeConfigRepo.find({ where: { createdBy: caller.userId } });
      createdSites.forEach((cs) => assignedCodes.add(cs.officeId));

      const allUserSites = await this.userSiteRepo.find();
      const inScopeUserIds = new Set(
        allUserSites.filter((us) => assignedCodes.has(us.siteCode)).map((us) => us.userId),
      );
      const allUsers = await this.userRepo.find();
      allUsers.forEach((u) => {
        if (assignedCodes.has(u.siteCode)) inScopeUserIds.add(u.userId);
      });

      const allowedIds = Array.from(inScopeUserIds);
      if (allowedIds.length === 0) return [];
      qb.andWhere('session.userId IN (:...allowedIds)', { allowedIds });
    }

    if (userId) {
      qb.andWhere('session.userId = :userId', { userId });
    }

    return qb.getMany();
  }
}
