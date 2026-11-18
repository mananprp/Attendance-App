import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, In } from 'typeorm';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { UserEntity } from '../../database/entities/user.entity';
import { UserSiteEntity } from '../../database/entities/user-site.entity';
import { AppConfigResponseDto, OfficeSiteDto } from './dto/app-config-response.dto';

@Injectable()
export class ConfigService {
  constructor(
    @InjectRepository(OfficeConfigEntity)
    private readonly officeConfigRepository: Repository<OfficeConfigEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(UserSiteEntity)
    private readonly userSiteRepository: Repository<UserSiteEntity>,
  ) {}

  async getAppConfig(userId?: string): Promise<AppConfigResponseDto> {
    let siteCode = 'DEFAULT_OFFICE';
    const siteCodesSet = new Set<string>();

    if (userId) {
      const user = await this.userRepository.findOne({
        where: { userId },
        select: ['siteCode'],
      });
      if (user?.siteCode) {
        siteCode = user.siteCode;
        siteCodesSet.add(user.siteCode);
      }

      const userSites = await this.userSiteRepository.find({ where: { userId } });
      userSites.forEach((us) => siteCodesSet.add(us.siteCode));
    }

    if (siteCodesSet.size === 0) {
      siteCodesSet.add(siteCode);
    }

    const assignedSiteCodes = Array.from(siteCodesSet);
    const assignedOffices = await this.officeConfigRepository.find({
      where: { officeId: In(assignedSiteCodes) },
    });

    let config = assignedOffices.find((o) => o.officeId === siteCode) || assignedOffices[0];

    // Fallback to DEFAULT_OFFICE if specific site is not found
    if (!config && siteCode !== 'DEFAULT_OFFICE') {
      config = await this.officeConfigRepository.findOne({
        where: { officeId: 'DEFAULT_OFFICE' },
      });
    }

    const assignedSitesDto: OfficeSiteDto[] = assignedOffices.map((o) => ({
      office_id: o.officeId,
      name: o.name,
      latitude: Number(o.latitude),
      longitude: Number(o.longitude),
      radius_m: o.radiusM,
    }));

    if (config) {
      return {
        office_lat: Number(config.latitude),
        office_lng: Number(config.longitude),
        radius_m: config.radiusM,
        timezone: config.timezone,
        sync_windows: config.syncWindows,
        sync_jitter_minutes: config.syncJitterMinutes,
        match_threshold: Number(config.matchThreshold) || 0.38,
        assigned_sites: assignedSitesDto.length > 0 ? assignedSitesDto : undefined,
      };
    }

    // Default fallback if database has not yet been seeded
    return {
      office_lat: 28.490293,
      office_lng: 77.060544,
      radius_m: 100,
      timezone: 'Asia/Kolkata',
      sync_windows: ['16:00', '00:00'],
      sync_jitter_minutes: 30,
      match_threshold: 0.38,
      assigned_sites: assignedSitesDto.length > 0 ? assignedSitesDto : undefined,
    };
  }
}
