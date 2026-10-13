import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { UserEntity } from '../../database/entities/user.entity';
import { AppConfigResponseDto } from './dto/app-config-response.dto';

@Injectable()
export class ConfigService {
  constructor(
    @InjectRepository(OfficeConfigEntity)
    private readonly officeConfigRepository: Repository<OfficeConfigEntity>,
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
  ) {}

  async getAppConfig(userId?: string): Promise<AppConfigResponseDto> {
    let siteCode = 'DEFAULT_OFFICE';
    if (userId) {
      const user = await this.userRepository.findOne({
        where: { userId },
        select: ['siteCode'],
      });
      if (user?.siteCode) {
        siteCode = user.siteCode;
      }
    }

    let config = await this.officeConfigRepository.findOne({
      where: { officeId: siteCode },
    });

    // Fallback to DEFAULT_OFFICE if specific site is not found
    if (!config && siteCode !== 'DEFAULT_OFFICE') {
      config = await this.officeConfigRepository.findOne({
        where: { officeId: 'DEFAULT_OFFICE' },
      });
    }

    if (config) {
      return {
        office_lat: Number(config.latitude),
        office_lng: Number(config.longitude),
        radius_m: config.radiusM,
        timezone: config.timezone,
        sync_windows: config.syncWindows,
        sync_jitter_minutes: config.syncJitterMinutes,
        match_threshold: 0.46,
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
      match_threshold: 0.46,
    };
  }
}
