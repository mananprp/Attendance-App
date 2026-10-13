import {
  Injectable,
  NotFoundException,
  ConflictException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcrypt';
import { UserEntity, UserRole, UserStatus } from '../../database/entities/user.entity';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { AdminUserDto } from './dto/admin-user.dto';
import { AdminSiteDto } from './dto/admin-site.dto';
import { CreateUserDto } from './dto/create-user.dto';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(OfficeConfigEntity)
    private readonly officeConfigRepository: Repository<OfficeConfigEntity>,
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
      match_threshold: 0.46,
    }));
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
}
