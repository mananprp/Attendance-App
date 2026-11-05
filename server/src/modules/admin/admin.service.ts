import {
  Injectable,
  NotFoundException,
  ConflictException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { UserEntity, UserRole, UserStatus } from '../../database/entities/user.entity';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { AttendancePunchEntity, LocationType, PunchType } from '../../database/entities/attendance-punch.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import { AdminUserDto } from './dto/admin-user.dto';
import { AdminSiteDto } from './dto/admin-site.dto';
import { CreateUserDto } from './dto/create-user.dto';
import { CreateSiteDto } from './dto/create-site.dto';
import { AdminPunchDto } from './dto/admin-punch.dto';
import { AdminPunchFilterDto } from './dto/admin-punch-filter.dto';
import { AdminDailyAttendanceDto } from './dto/admin-daily-attendance.dto';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(OfficeConfigEntity)
    private readonly officeConfigRepository: Repository<OfficeConfigEntity>,
    @InjectRepository(AttendancePunchEntity)
    private readonly punchRepository: Repository<AttendancePunchEntity>,
    @InjectRepository(UserPhotoEntity)
    private readonly photoRepository: Repository<UserPhotoEntity>,
  ) {}

  async getSites(): Promise<AdminSiteDto[]> {
    const offices = await this.officeConfigRepository.find();
    return offices.map((o) => ({
      office_id: o.officeId,
      name: o.name,
      latitude: Number(o.latitude),
      longitude: Number(o.longitude),
      radius_m: o.radiusM,
      timezone: o.timezone,
      match_threshold: Number(o.matchThreshold) || 0.38,
    }));
  }

  async createSite(dto: CreateSiteDto): Promise<AdminSiteDto> {
    const officeId = dto.office_id.trim().toUpperCase();
    const existing = await this.officeConfigRepository.findOne({
      where: { officeId },
    });
    if (existing) {
      throw new ConflictException({
        code: 'SITE_ALREADY_EXISTS',
        message: `Office site with ID '${officeId}' already exists.`,
      });
    }

    const site = this.officeConfigRepository.create({
      officeId,
      name: dto.name.trim(),
      latitude: dto.latitude,
      longitude: dto.longitude,
      radiusM: dto.radius_m || 100,
      timezone: dto.timezone || 'Asia/Kolkata',
      syncWindows: ['16:00', '00:00'],
      syncJitterMinutes: 30,
      matchThreshold: 0.38,
    });

    const saved = await this.officeConfigRepository.save(site);

    return {
      office_id: saved.officeId,
      name: saved.name,
      latitude: Number(saved.latitude),
      longitude: Number(saved.longitude),
      radius_m: saved.radiusM,
      timezone: saved.timezone,
      match_threshold: Number(saved.matchThreshold) || 0.38,
    };
  }

  async getUsers(): Promise<AdminUserDto[]> {
    const users = await this.userRepository.find({
      order: { createdAt: 'DESC' },
    });
    const offices = await this.officeConfigRepository.find();
    const officeMap = new Map(offices.map((o) => [o.officeId, o.name]));

    return users.map((u) => ({
      user_id: u.userId,
      name: u.name,
      role: u.role,
      site_code: u.siteCode || 'DEFAULT_OFFICE',
      site_name: officeMap.get(u.siteCode) || officeMap.get('DEFAULT_OFFICE') || 'Headquarters',
      status: u.status,
      photo_change_used: u.photoChangeUsed,
    }));
  }

  async createUser(dto: CreateUserDto): Promise<AdminUserDto> {
    const existing = await this.userRepository.findOne({
      where: { userId: dto.user_id },
    });
    if (existing) {
      throw new ConflictException({
        code: 'USER_ALREADY_EXISTS',
        message: `User with ID '${dto.user_id}' already exists.`,
      });
    }

    const siteCode = dto.site_code || 'DEFAULT_OFFICE';
    const site = await this.officeConfigRepository.findOne({
      where: { officeId: siteCode },
    });
    if (!site && siteCode !== 'DEFAULT_OFFICE') {
      throw new BadRequestException({
        code: 'SITE_NOT_FOUND',
        message: `Office site '${siteCode}' does not exist.`,
      });
    }

    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(dto.password, saltRounds);

    const user = this.userRepository.create({
      userId: dto.user_id.trim(),
      name: dto.name.trim(),
      passwordHash,
      role: dto.role || UserRole.EMPLOYEE,
      siteCode: siteCode,
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    });

    const saved = await this.userRepository.save(user);

    return {
      user_id: saved.userId,
      name: saved.name,
      role: saved.role,
      site_code: saved.siteCode,
      site_name: site?.name || 'Headquarters',
      status: saved.status,
      photo_change_used: saved.photoChangeUsed,
    };
  }

  async updateUserSite(userId: string, siteCode: string): Promise<AdminUserDto> {
    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    const site = await this.officeConfigRepository.findOne({
      where: { officeId: siteCode },
    });
    if (!site) {
      throw new BadRequestException({
        code: 'SITE_NOT_FOUND',
        message: `Office site '${siteCode}' does not exist.`,
      });
    }

    user.siteCode = siteCode;
    const saved = await this.userRepository.save(user);

    return {
      user_id: saved.userId,
      name: saved.name,
      role: saved.role,
      site_code: saved.siteCode,
      site_name: site.name,
      status: saved.status,
      photo_change_used: saved.photoChangeUsed,
    };
  }

  async updateUserRole(userId: string, role: UserRole): Promise<AdminUserDto> {
    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    user.role = role;
    const saved = await this.userRepository.save(user);

    const site = await this.officeConfigRepository.findOne({
      where: { officeId: saved.siteCode },
    });

    return {
      user_id: saved.userId,
      name: saved.name,
      role: saved.role,
      site_code: saved.siteCode,
      site_name: site?.name || 'Headquarters',
      status: saved.status,
      photo_change_used: saved.photoChangeUsed,
    };
  }

  async updateUserPassword(userId: string, newPassword: string): Promise<AdminUserDto> {
    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    const saltRounds = 10;
    user.passwordHash = await bcrypt.hash(newPassword, saltRounds);
    const saved = await this.userRepository.save(user);

    const site = await this.officeConfigRepository.findOne({
      where: { officeId: saved.siteCode },
    });

    return {
      user_id: saved.userId,
      name: saved.name,
      role: saved.role,
      site_code: saved.siteCode,
      site_name: site?.name || 'Headquarters',
      status: saved.status,
      photo_change_used: saved.photoChangeUsed,
    };
  }

  async deleteUser(userId: string, adminUserId?: string): Promise<{ success: boolean; message: string }> {
    if (adminUserId && userId.toLowerCase() === adminUserId.toLowerCase()) {
      throw new BadRequestException({
        code: 'CANNOT_DELETE_SELF',
        message: 'You cannot delete your own account.',
      });
    }

    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    // Clean up punches first if any
    await this.punchRepository.delete({ userId });
    await this.userRepository.remove(user);

    return {
      success: true,
      message: `User '${userId}' deleted successfully.`,
    };
  }

  async getAllPunches(filter: AdminPunchFilterDto): Promise<AdminPunchDto[]> {
    const qb = this.punchRepository
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.user', 'u')
      .orderBy('p.punchTimeUtc', 'DESC');

    if (filter.user_id && filter.user_id.trim()) {
      qb.andWhere('p.userId = :userId', { userId: filter.user_id.trim() });
    }
    if (filter.from_date && filter.from_date.trim()) {
      qb.andWhere('p.dateLocal >= :fromDate', { fromDate: filter.from_date.trim() });
    }
    if (filter.to_date && filter.to_date.trim()) {
      qb.andWhere('p.dateLocal <= :toDate', { toDate: filter.to_date.trim() });
    }
    if (filter.site_code && filter.site_code.trim()) {
      qb.andWhere('u.siteCode = :siteCode', { siteCode: filter.site_code.trim() });
    }

    const punches = await qb.getMany();

    return punches.map((p) => {
      const utcDate = p.punchTimeUtc instanceof Date ? p.punchTimeUtc : new Date(p.punchTimeUtc);
      const localTimeStr = new Intl.DateTimeFormat('en-IN', {
        timeZone: 'Asia/Kolkata',
        dateStyle: 'medium',
        timeStyle: 'medium',
      }).format(utcDate);

      const lat = Number(p.latitude);
      const lng = Number(p.longitude);

      return {
        punch_uuid: p.punchUuid,
        user_id: p.userId,
        user_name: p.user?.name || p.userId,
        site_code: p.user?.siteCode || 'DEFAULT_OFFICE',
        date_local: p.dateLocal,
        type: p.type,
        location_type: p.locationType || LocationType.OFFICE,
        punch_time_utc: utcDate.toISOString(),
        punch_time_local: localTimeStr,
        time_source: p.timeSource,
        time_unverified: p.timeUnverified,
        latitude: lat,
        longitude: lng,
        maps_url: `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`,
        distance_m: Number(p.distanceM),
        match_score: Number(p.matchScore),
      };
    });
  }

  async exportPunchesCsv(filter: AdminPunchFilterDto): Promise<string> {
    const punches = await this.getAllPunches(filter);
    const headers = [
      'Punch UUID',
      'Employee ID',
      'Employee Name',
      'Site Code',
      'Date (Local)',
      'Punch Type',
      'Location Type',
      'Punch Time (IST)',
      'Time Source',
      'Time Unverified',
      'Latitude',
      'Longitude',
      'Distance (m)',
      'Match Score',
      'Google Maps URL',
    ];

    const escapeCsv = (val: any) => {
      if (val === null || val === undefined) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = punches.map((p) =>
      [
        escapeCsv(p.punch_uuid),
        escapeCsv(p.user_id),
        escapeCsv(p.user_name),
        escapeCsv(p.site_code),
        escapeCsv(p.date_local),
        escapeCsv(p.type),
        escapeCsv(p.location_type),
        escapeCsv(p.punch_time_local),
        escapeCsv(p.time_source),
        escapeCsv(p.time_unverified ? 'YES' : 'NO'),
        escapeCsv(p.latitude),
        escapeCsv(p.longitude),
        escapeCsv(p.distance_m),
        escapeCsv(p.match_score),
        escapeCsv(p.maps_url),
      ].join(','),
    );

    return [headers.join(','), ...rows].join('\r\n');
  }

  async getAllAttendance(filter: AdminPunchFilterDto): Promise<AdminDailyAttendanceDto[]> {
    const qb = this.punchRepository
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.user', 'u')
      .orderBy('p.dateLocal', 'DESC')
      .addOrderBy('p.punchTimeUtc', 'ASC');

    if (filter.user_id && filter.user_id.trim()) {
      qb.andWhere('p.userId = :userId', { userId: filter.user_id.trim() });
    }
    if (filter.from_date && filter.from_date.trim()) {
      qb.andWhere('p.dateLocal >= :fromDate', { fromDate: filter.from_date.trim() });
    }
    if (filter.to_date && filter.to_date.trim()) {
      qb.andWhere('p.dateLocal <= :toDate', { toDate: filter.to_date.trim() });
    }
    if (filter.shift_type && filter.shift_type.trim()) {
      qb.andWhere('p.shiftType = :shiftType', { shiftType: filter.shift_type.trim().toUpperCase() });
    }
    if (filter.location_type && filter.location_type.trim()) {
      const loc = filter.location_type.trim().toUpperCase();
      if (loc === 'NEW_LOCATION') {
        qb.andWhere('p.locationType = :locType', { locType: LocationType.NEW_LOCATION });
      } else if (loc === 'OFFICE') {
        qb.andWhere('p.locationType = :locType', { locType: LocationType.OFFICE });
      }
    }
    if (filter.site_code && filter.site_code.trim()) {
      const site = filter.site_code.trim();
      if (site === 'NEW_LOCATION') {
        qb.andWhere('p.locationType = :locType', { locType: LocationType.NEW_LOCATION });
      } else if (site === 'OFFICE') {
        qb.andWhere('p.locationType = :locType', { locType: LocationType.OFFICE });
      } else {
        qb.andWhere('(u.siteCode = :siteCode AND p.locationType = :locType)', {
          siteCode: site,
          locType: LocationType.OFFICE,
        });
      }
    }

    const punches = await qb.getMany();

    const groupMap = new Map<string, typeof punches>();
    for (const p of punches) {
      const key = `${p.userId}_${p.dateLocal}`;
      if (!groupMap.has(key)) {
        groupMap.set(key, []);
      }
      groupMap.get(key)!.push(p);
    }

    const results: AdminDailyAttendanceDto[] = [];
    const nowUtcMs = Date.now();

    for (const [, groupPunches] of groupMap.entries()) {
      const inPunches = groupPunches.filter((p) => p.type === PunchType.IN);
      const outPunches = groupPunches.filter((p) => p.type === PunchType.OUT);

      const firstIn = inPunches.length > 0 ? inPunches[0] : null;
      const lastOut = outPunches.length > 0 ? outPunches[outPunches.length - 1] : null;

      const refPunch = firstIn || lastOut || groupPunches[0];
      const userId = refPunch.userId;
      const userName = refPunch.user?.name || userId;
      const siteCode = refPunch.user?.siteCode || 'DEFAULT_OFFICE';
      const dateLocal = refPunch.dateLocal;

      let shiftType = (refPunch as any).shiftType || 'GENERAL';
      if (!shiftType || shiftType === 'GENERAL') {
        const timeRef = firstIn?.punchTimeUtc || lastOut?.punchTimeUtc || refPunch.punchTimeUtc;
        if (timeRef) {
          const punchDate = timeRef instanceof Date ? timeRef : new Date(timeRef);
          const istHour = parseInt(
            new Intl.DateTimeFormat('en-IN', {
              timeZone: 'Asia/Kolkata',
              hour: 'numeric',
              hour12: false,
            }).format(punchDate),
            10
          );
          if (istHour >= 13 && istHour <= 18) {
            shiftType = 'EVENING';
          } else if (istHour >= 19 || istHour < 6) {
            shiftType = 'NIGHT';
          }
        }
      }
      const locationType = refPunch.locationType || LocationType.OFFICE;

      const lat = Number(firstIn?.latitude ?? lastOut?.latitude ?? refPunch.latitude);
      const lng = Number(firstIn?.longitude ?? lastOut?.longitude ?? refPunch.longitude);
      const dist = Number(firstIn?.distanceM ?? lastOut?.distanceM ?? refPunch.distanceM);

      let inTimeLocal: string | null = null;
      let inTimeUtc: string | null = null;
      if (firstIn) {
        const inDate = firstIn.punchTimeUtc instanceof Date ? firstIn.punchTimeUtc : new Date(firstIn.punchTimeUtc);
        inTimeUtc = inDate.toISOString();
        inTimeLocal = new Intl.DateTimeFormat('en-IN', {
          timeZone: 'Asia/Kolkata',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: true,
        }).format(inDate);
      }

      let outTimeLocal: string | null = null;
      let outTimeUtc: string | null = null;
      if (lastOut) {
        const outDate = lastOut.punchTimeUtc instanceof Date ? lastOut.punchTimeUtc : new Date(lastOut.punchTimeUtc);
        outTimeUtc = outDate.toISOString();
        outTimeLocal = new Intl.DateTimeFormat('en-IN', {
          timeZone: 'Asia/Kolkata',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: true,
        }).format(outDate);
      }

      const [y, m, d] = dateLocal.split('-').map(Number);
      const nextDayDate = new Date(Date.UTC(y, m - 1, d + 1));
      let cutoffHourIst = 3;
      if (shiftType === 'EVENING') cutoffHourIst = 6;
      if (shiftType === 'NIGHT') cutoffHourIst = 12;

      const cutoffUtcMs = Date.UTC(
        nextDayDate.getUTCFullYear(),
        nextDayDate.getUTCMonth(),
        nextDayDate.getUTCDate(),
        cutoffHourIst,
        0,
        0
      ) - (5.5 * 3600 * 1000);

      let status = 'IN_PROGRESS';
      let workDurationFormatted = '—';
      let workDurationMinutes: number | null = null;

      if (firstIn && lastOut) {
        status = 'COMPLETE';
        const inMs = new Date(firstIn.punchTimeUtc).getTime();
        const outMs = new Date(lastOut.punchTimeUtc).getTime();
        const diffMs = Math.max(0, outMs - inMs);
        workDurationMinutes = Math.round(diffMs / 60000);
        const hrs = Math.floor(workDurationMinutes / 60);
        const mins = workDurationMinutes % 60;
        workDurationFormatted = `${hrs}h ${mins}m`;
      } else if (firstIn && !lastOut) {
        if (nowUtcMs > cutoffUtcMs) {
          status = 'MISSED_PUNCH';
          workDurationFormatted = 'Missed Punch';
        } else {
          status = 'IN_PROGRESS';
          workDurationFormatted = 'In Progress';
        }
      } else if (!firstIn && lastOut) {
        status = 'MISSED_PUNCH';
        workDurationFormatted = 'Missed In';
      }

      const scores = groupPunches.map((p) => Number(p.matchScore)).filter((s) => !isNaN(s));
      const matchScoreAvg = scores.length > 0 ? Number((scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(4)) : null;

      results.push({
        user_id: userId,
        user_name: userName,
        site_code: siteCode,
        date_local: dateLocal,
        shift_type: shiftType,
        in_time_local: inTimeLocal,
        in_time_utc: inTimeUtc,
        out_time_local: outTimeLocal,
        out_time_utc: outTimeUtc,
        work_duration_formatted: workDurationFormatted,
        work_duration_minutes: workDurationMinutes,
        location_type: locationType,
        latitude: lat,
        longitude: lng,
        maps_url: `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`,
        distance_m: dist,
        status,
        match_score_avg: matchScoreAvg,
      });
    }

    let filteredResults = results;
    if (filter.status && filter.status.trim()) {
      const targetStatus = filter.status.trim().toUpperCase();
      filteredResults = filteredResults.filter((r) => r.status === targetStatus);
    }

    filteredResults.sort((a, b) => {
      if (a.date_local !== b.date_local) {
        return b.date_local.localeCompare(a.date_local);
      }
      return a.user_id.localeCompare(b.user_id);
    });

    return filteredResults;
  }

  async exportAttendanceCsv(filter: AdminPunchFilterDto): Promise<string> {
    const records = await this.getAllAttendance(filter);
    const headers = [
      'Employee ID',
      'Employee Name',
      'Site Code',
      'Date',
      'Shift',
      'Punch In Time',
      'Punch Out Time',
      'Work Duration',
      'Location Type',
      'Latitude',
      'Longitude',
      'Distance (m)',
      'Status',
      'Match Score',
      'Google Maps URL',
    ];

    const escapeCsv = (val: any) => {
      if (val === null || val === undefined) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = records.map((r) =>
      [
        escapeCsv(r.user_id),
        escapeCsv(r.user_name),
        escapeCsv(r.site_code),
        escapeCsv(r.date_local),
        escapeCsv(r.shift_type),
        escapeCsv(r.in_time_local || '—'),
        escapeCsv(r.out_time_local || '—'),
        escapeCsv(r.work_duration_formatted),
        escapeCsv(r.location_type),
        escapeCsv(r.latitude),
        escapeCsv(r.longitude),
        escapeCsv(r.distance_m != null ? `${r.distance_m}m` : '—'),
        escapeCsv(r.status),
        escapeCsv(r.match_score_avg != null ? `${(r.match_score_avg * 100).toFixed(0)}%` : '—'),
        escapeCsv(r.maps_url),
      ].join(','),
    );

    return [headers.join(','), ...rows].join('\r\n');
  }
  async resetUserPhoto(userId: string): Promise<{ success: boolean; message: string }> {
    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    await this.photoRepository.delete({ userId });
    user.photoChangeUsed = false;
    await this.userRepository.save(user);

    return {
      success: true,
      message: `Profile photo for user '${userId}' reset successfully. User will be asked to re-enrol their photo at next login.`,
    };
  }

  async getUserPhotoFile(userId: string): Promise<{ data: Buffer; mimeType: string; hash: string }> {
    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: `User '${userId}' not found.`,
      });
    }

    const photo = await this.photoRepository
      .createQueryBuilder('photo')
      .addSelect('photo.imageData')
      .where('photo.userId = :userId', { userId })
      .andWhere('photo.isActive = :isActive', { isActive: true })
      .andWhere('photo.imageData IS NOT NULL')
      .orderBy('photo.version', 'DESC')
      .getOne();

    if (!photo || !photo.imageData) {
      throw new NotFoundException({
        code: 'PHOTO_NOT_FOUND',
        message: `No active profile photo found for user '${userId}'.`,
      });
    }

    return {
      data: photo.imageData,
      mimeType: photo.mimeType || 'image/jpeg',
      hash: photo.hash,
    };
  }
}
