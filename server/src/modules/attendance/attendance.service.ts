import {
  Injectable,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { v4 as uuidv4 } from 'uuid';
import * as fs from 'fs';
import * as path from 'path';
import {
  AttendancePunchEntity,
  PunchType,
  TimeSource,
  LocationType,
} from '../../database/entities/attendance-punch.entity';
import {
  AttendanceDayEntity,
  AttendanceDayStatus,
} from '../../database/entities/attendance-day.entity';
import {
  SyncBatchEntity,
  SyncBatchStatus,
} from '../../database/entities/sync-batch.entity';
import { SyncPunchItemDto } from './dto/sync-punch-item.dto';
import {
  SyncBatchResponseDto,
  PunchSyncStatus,
  PunchSyncResultDto,
} from './dto/sync-batch-response.dto';
import {
  MonthlyAttendanceResponseDto,
  DaySummaryDto,
} from './dto/monthly-attendance-response.dto';

@Injectable()
export class AttendanceService {
  private readonly punchesUploadDir = path.resolve(
    process.cwd(),
    'uploads',
    'punches',
  );

  constructor(
    @InjectRepository(AttendancePunchEntity)
    private readonly punchRepository: Repository<AttendancePunchEntity>,
    @InjectRepository(AttendanceDayEntity)
    private readonly dayRepository: Repository<AttendanceDayEntity>,
    @InjectRepository(SyncBatchEntity)
    private readonly syncBatchRepository: Repository<SyncBatchEntity>,
  ) {
    if (!fs.existsSync(this.punchesUploadDir)) {
      fs.mkdirSync(this.punchesUploadDir, { recursive: true });
    }
  }

  async syncPunches(
    userId: string,
    batchJsonStr: string,
    files: Express.Multer.File[] = [],
  ): Promise<SyncBatchResponseDto> {
    if (!batchJsonStr) {
      throw new BadRequestException({
        code: 'INVALID_PAYLOAD',
        message: 'batch_json is required.',
      });
    }

    let items: SyncPunchItemDto[];
    try {
      items = typeof batchJsonStr === 'string' ? JSON.parse(batchJsonStr) : batchJsonStr;
    } catch {
      throw new BadRequestException({
        code: 'INVALID_JSON',
        message: 'batch_json must be a valid JSON array string.',
      });
    }

    if (!Array.isArray(items)) {
      throw new BadRequestException({
        code: 'INVALID_PAYLOAD',
        message: 'batch_json must be an array of punch items.',
      });
    }

    const batchId = uuidv4();
    const results: PunchSyncResultDto[] = [];
    const affectedDates = new Set<string>();

    // 1. Create and persist initial batch audit record first (to satisfy FK constraint)
    const batchEntity = this.syncBatchRepository.create({
      batchId,
      userId,
      recordsCount: items.length,
      status: SyncBatchStatus.SUCCESS,
    });
    await this.syncBatchRepository.save(batchEntity);

    // Map files by normalized filename/uuid
    const fileMap = new Map<string, Express.Multer.File>();
    for (const file of files) {
      fileMap.set(file.originalname, file);
      const nameWithoutExt = path.parse(file.originalname).name;
      fileMap.set(nameWithoutExt, file);
    }

    for (const item of items) {
      if (!item.punch_uuid || !item.date_local || !item.type || !item.punch_time_utc) {
        results.push({
          punch_uuid: item.punch_uuid || 'unknown',
          status: PunchSyncStatus.REJECTED,
          reason: 'Missing required punch fields.',
        });
        continue;
      }

      // Check for duplicate punch
      const existingPunch = await this.punchRepository.findOne({
        where: { punchUuid: item.punch_uuid },
      });

      if (existingPunch) {
        results.push({
          punch_uuid: item.punch_uuid,
          status: PunchSyncStatus.DUPLICATE,
          reason: 'Punch record already synced',
        });
        continue;
      }

      // Handle image attachment if available
      let imagePath: string | null = null;
      const matchedFile =
        fileMap.get(item.punch_uuid) ||
        (item.image_file_name ? fileMap.get(item.image_file_name) : null) ||
        fileMap.get(`${item.punch_uuid}.jpg`) ||
        fileMap.get(`${item.punch_uuid}.jpeg`) ||
        fileMap.get(`${item.punch_uuid}.png`);

      if (matchedFile) {
        const ext = path.extname(matchedFile.originalname) || '.jpg';
        const filename = `${item.punch_uuid}${ext}`;
        const targetPath = path.join(this.punchesUploadDir, filename);
        fs.writeFileSync(targetPath, matchedFile.buffer);
        imagePath = `/uploads/punches/${filename}`;
      }

      const rawLocType = (item.location_type || (item as any).locationType || '').toString().trim().toUpperCase();
      const resolvedLocType = rawLocType === 'NEW_LOCATION' ? LocationType.NEW_LOCATION : LocationType.OFFICE;
      const rawShiftType = (item.shift_type || (item as any).shiftType || '').toString().trim().toUpperCase();
      let resolvedShiftType = ['GENERAL', 'EVENING', 'NIGHT'].includes(rawShiftType) ? rawShiftType : '';
      if (!resolvedShiftType || resolvedShiftType === 'GENERAL') {
        const punchDate = new Date(item.punch_time_utc);
        const istHour = parseInt(
          new Intl.DateTimeFormat('en-IN', {
            timeZone: 'Asia/Kolkata',
            hour: 'numeric',
            hour12: false,
          }).format(punchDate),
          10
        );
        if (istHour >= 13 && istHour <= 18) {
          resolvedShiftType = 'EVENING';
        } else if (istHour >= 19 || istHour < 6) {
          resolvedShiftType = 'NIGHT';
        } else {
          resolvedShiftType = 'GENERAL';
        }
      }

      // Persist punch
      const punchEntity = this.punchRepository.create({
        punchUuid: item.punch_uuid,
        userId: userId,
        dateLocal: item.date_local,
        type: item.type as PunchType,
        locationType: resolvedLocType,
        shiftType: resolvedShiftType,
        punchTimeUtc: new Date(item.punch_time_utc),
        timeSource: (item.time_source as TimeSource) || TimeSource.NTP,
        timeUnverified: Boolean(item.time_unverified),
        latitude: item.latitude,
        longitude: item.longitude,
        distanceM: item.distance_m,
        matchScore: item.match_score,
        imagePath,
        syncBatchId: batchId,
      });

      await this.punchRepository.save(punchEntity);
      affectedDates.add(item.date_local);

      results.push({
        punch_uuid: item.punch_uuid,
        status: PunchSyncStatus.ACCEPTED,
        reason: null,
      });
    }

    // Recalculate daily summary for affected dates
    for (const dateLocal of affectedDates) {
      await this.recalculateDaySummary(userId, dateLocal);
    }

    // Update batch status if any rejected
    const hasRejected = results.some((r) => r.status === PunchSyncStatus.REJECTED);
    const hasAccepted = results.some((r) => r.status === PunchSyncStatus.ACCEPTED);
    if (hasRejected && hasAccepted) {
      batchEntity.status = SyncBatchStatus.PARTIAL;
      await this.syncBatchRepository.save(batchEntity);
    } else if (hasRejected && !hasAccepted) {
      batchEntity.status = SyncBatchStatus.FAILED;
      await this.syncBatchRepository.save(batchEntity);
    }

    return {
      batch_id: batchId,
      processed_count: items.length,
      results,
    };
  }

  async recalculateDaySummary(userId: string, dateLocal: string): Promise<void> {
    const punches = await this.punchRepository.find({
      where: { userId, dateLocal },
      order: { punchTimeUtc: 'ASC' },
    });

    const inPunches = punches.filter((p) => p.type === PunchType.IN);
    const outPunches = punches.filter((p) => p.type === PunchType.OUT);

    const firstIn = inPunches.length > 0 ? inPunches[0].punchTimeUtc : null;
    const lastOut = outPunches.length > 0 ? outPunches[outPunches.length - 1].punchTimeUtc : null;
    const shiftType = inPunches.length > 0 && inPunches[0].shiftType ? inPunches[0].shiftType : 'GENERAL';

    // Calculate cutoff in IST
    const [year, month, day] = dateLocal.split('-').map(Number);
    const nextDayDate = new Date(Date.UTC(year, month - 1, day + 1));
    let cutoffHourIst = 3; // General: 03:00 AM next day
    if (shiftType === 'EVENING') cutoffHourIst = 6; // Evening: 06:00 AM next day
    if (shiftType === 'NIGHT') cutoffHourIst = 12; // Night: 12:00 PM noon next day

    // Cutoff in UTC: IST is UTC + 5:30 -> UTC = IST - 5:30
    const cutoffUtcMs = Date.UTC(
      nextDayDate.getUTCFullYear(),
      nextDayDate.getUTCMonth(),
      nextDayDate.getUTCDate(),
      cutoffHourIst,
      0,
      0
    ) - (5.5 * 3600 * 1000);

    const nowUtcMs = Date.now();

    let status = AttendanceDayStatus.NO_RECORD;
    if (firstIn && lastOut) {
      status = AttendanceDayStatus.COMPLETE;
    } else if (firstIn || lastOut) {
      if (nowUtcMs > cutoffUtcMs) {
        status = AttendanceDayStatus.MISSED_PUNCH;
      } else {
        status = AttendanceDayStatus.NO_RECORD; // still active
      }
    }

    const timeUnverified = punches.some((p) => p.timeUnverified);

    let dayRecord = await this.dayRepository.findOne({
      where: { userId, date: dateLocal },
    });

    if (!dayRecord) {
      dayRecord = this.dayRepository.create({
        userId,
        date: dateLocal,
        inTime: firstIn,
        outTime: lastOut,
        status,
        timeUnverified,
        shiftType,
      });
    } else {
      dayRecord.inTime = firstIn;
      dayRecord.outTime = lastOut;
      dayRecord.status = status;
      dayRecord.timeUnverified = timeUnverified;
      dayRecord.shiftType = shiftType;
    }

    await this.dayRepository.save(dayRecord);
  }

  async getMonthlyAttendance(
    userId: string,
    month: string,
  ): Promise<MonthlyAttendanceResponseDto> {
    const monthRegex = /^\d{4}-(0[1-9]|1[0-2])$/;
    if (!month || !monthRegex.test(month)) {
      throw new BadRequestException({
        code: 'INVALID_MONTH_FORMAT',
        message: 'Target month must match format YYYY-MM (e.g. 2026-09).',
      });
    }

    const dayEntities = await this.dayRepository.find({
      where: { userId, date: Like(`${month}-%`) },
      order: { date: 'ASC' },
    });

    const punchEntities = await this.punchRepository.find({
      where: { userId, dateLocal: Like(`${month}-%`) },
      order: { punchTimeUtc: 'ASC' },
    });

    const days: DaySummaryDto[] = dayEntities.map((d) => ({
      date_local: d.date,
      in_time: d.inTime ? (d.inTime instanceof Date ? d.inTime.toISOString() : new Date(d.inTime).toISOString()) : null,
      out_time: d.outTime ? (d.outTime instanceof Date ? d.outTime.toISOString() : new Date(d.outTime).toISOString()) : null,
      status: d.status,
      time_unverified: d.timeUnverified,
    }));

    const punches: SyncPunchItemDto[] = punchEntities.map((p) => ({
      punch_uuid: p.punchUuid,
      user_id: p.userId,
      date_local: p.dateLocal,
      type: p.type,
      location_type: p.locationType || LocationType.OFFICE,
      punch_time_utc: p.punchTimeUtc instanceof Date ? p.punchTimeUtc.toISOString() : new Date(p.punchTimeUtc).toISOString(),
      time_source: p.timeSource,
      time_unverified: p.timeUnverified,
      latitude: Number(p.latitude),
      longitude: Number(p.longitude),
      distance_m: Number(p.distanceM),
      match_score: Number(p.matchScore),
      image_file_name: p.imagePath ? path.basename(p.imagePath) : null,
    }));

    return {
      month,
      days,
      punches,
    };
  }
}
