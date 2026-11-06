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
} from '../../database/entities';
import { StartTrackingSessionDto } from './dto/start-tracking-session.dto';
import { FinishTrackingSessionDto } from './dto/finish-tracking-session.dto';

@Injectable()
export class TrackingService {
  constructor(
    @InjectRepository(TrackingSessionEntity)
    private readonly sessionRepo: Repository<TrackingSessionEntity>,
    @InjectRepository(TrackingPointEntity)
    private readonly pointRepo: Repository<TrackingPointEntity>,
    @InjectRepository(TrackingStopEntity)
    private readonly stopRepo: Repository<TrackingStopEntity>,
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

  async getAdminSessions(userId?: string, limit = 50) {
    const qb = this.sessionRepo
      .createQueryBuilder('session')
      .leftJoinAndSelect('session.user', 'user')
      .orderBy('session.startTimeUtc', 'DESC')
      .take(limit);

    if (userId) {
      qb.andWhere('session.userId = :userId', { userId });
    }

    return qb.getMany();
  }
}
