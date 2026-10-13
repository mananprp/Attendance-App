import {
  Injectable,
  UnauthorizedException,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { UserEntity, UserStatus } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import { LoginRequestDto } from './dto/login-request.dto';
import { LoginResponseDto } from './dto/login-response.dto';

interface RateLimitRecord {
  count: number;
  resetAt: number;
}

@Injectable()
export class AuthService {
  private loginAttempts: Map<string, RateLimitRecord> = new Map();
  private readonly MAX_ATTEMPTS = 10;
  private readonly WINDOW_MS = 60 * 1000; // 1 minute window

  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(UserPhotoEntity)
    private readonly photoRepository: Repository<UserPhotoEntity>,
    private readonly jwtService: JwtService,
  ) {}

  private checkRateLimit(key: string) {
    const now = Date.now();
    const record = this.loginAttempts.get(key);

    if (record) {
      if (now > record.resetAt) {
        this.loginAttempts.set(key, { count: 1, resetAt: now + this.WINDOW_MS });
      } else {
        record.count++;
        if (record.count > this.MAX_ATTEMPTS) {
          throw new HttpException(
            {
              code: 'RATE_LIMIT_EXCEEDED',
              message: 'Too many login attempts. Please try again later.',
            },
            HttpStatus.TOO_MANY_REQUESTS,
          );
        }
      }
    } else {
      this.loginAttempts.set(key, { count: 1, resetAt: now + this.WINDOW_MS });
    }
  }

  async login(loginDto: LoginRequestDto, ipAddress?: string): Promise<LoginResponseDto> {
    const rateLimitKey = `${loginDto.user_id}_${ipAddress || 'default'}`;
    this.checkRateLimit(rateLimitKey);

    const user = await this.userRepository.findOne({
      where: { userId: loginDto.user_id },
    });

    if (!user) {
      throw new UnauthorizedException({
        code: 'INVALID_CREDENTIALS',
        message: 'The user ID or password provided is incorrect.',
      });
    }

    if (user.status !== UserStatus.ACTIVE) {
      throw new UnauthorizedException({
        code: 'ACCOUNT_INACTIVE',
        message: `Account is ${user.status.toLowerCase()}. Please contact administrator.`,
      });
    }

    const isMatch = await bcrypt.compare(loginDto.password, user.passwordHash);
    if (!isMatch) {
      throw new UnauthorizedException({
        code: 'INVALID_CREDENTIALS',
        message: 'The user ID or password provided is incorrect.',
      });
    }

    // Reset rate limit counter on successful login
    this.loginAttempts.delete(rateLimitKey);

    // Fetch active photo info
    const activePhoto = await this.photoRepository.findOne({
      where: { userId: user.userId, isActive: true },
      order: { version: 'DESC' },
    });

    const payload = {
      sub: user.userId,
      name: user.name,
      role: user.role,
      site_code: user.siteCode,
    };

    const token = this.jwtService.sign(payload);

    return {
      token,
      user: {
        user_id: user.userId,
        display_name: user.name,
        role: user.role,
        site_code: user.siteCode,
        photo_change_used: user.photoChangeUsed,
        has_photo: !!activePhoto,
        active_photo_version: activePhoto ? activePhoto.version : null,
      },
    };
  }
}
