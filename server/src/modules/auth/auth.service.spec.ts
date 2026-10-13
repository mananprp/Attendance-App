import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { JwtService } from '@nestjs/jwt';
import { UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { AuthService } from './auth.service';
import { UserEntity, UserStatus } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';

describe('AuthService', () => {
  let service: AuthService;
  let userRepo: any;
  let photoRepo: any;
  let jwtService: any;

  beforeEach(async () => {
    userRepo = {
      findOne: jest.fn(),
    };
    photoRepo = {
      findOne: jest.fn(),
    };
    jwtService = {
      sign: jest.fn().mockReturnValue('mock_jwt_token'),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        {
          provide: getRepositoryToken(UserEntity),
          useValue: userRepo,
        },
        {
          provide: getRepositoryToken(UserPhotoEntity),
          useValue: photoRepo,
        },
        {
          provide: JwtService,
          useValue: jwtService,
        },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
  });

  it('should successfully login with valid credentials', async () => {
    const passwordHash = await bcrypt.hash('Password@123', 10);
    const mockUser: Partial<UserEntity> = {
      userId: 'EMP1001',
      name: 'Alice Sharma',
      passwordHash,
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    };
    userRepo.findOne.mockResolvedValue(mockUser);
    photoRepo.findOne.mockResolvedValue({ version: 1, isActive: true });

    const result = await service.login({
      user_id: 'EMP1001',
      password: 'Password@123',
    });

    expect(result.token).toBe('mock_jwt_token');
    expect(result.user.user_id).toBe('EMP1001');
    expect(result.user.display_name).toBe('Alice Sharma');
    expect(result.user.has_photo).toBe(true);
    expect(result.user.active_photo_version).toBe(1);
    expect(result.user.photo_change_used).toBe(false);
  });

  it('should throw UnauthorizedException on invalid password', async () => {
    const passwordHash = await bcrypt.hash('CorrectPass', 10);
    userRepo.findOne.mockResolvedValue({
      userId: 'EMP1001',
      passwordHash,
      status: UserStatus.ACTIVE,
    });

    await expect(
      service.login({ user_id: 'EMP1001', password: 'WrongPassword' }),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('should throw UnauthorizedException on non-existent user', async () => {
    userRepo.findOne.mockResolvedValue(null);

    await expect(
      service.login({ user_id: 'NONEXISTENT', password: 'Password@123' }),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('should throw UnauthorizedException if account is inactive/suspended', async () => {
    const passwordHash = await bcrypt.hash('Password@123', 10);
    userRepo.findOne.mockResolvedValue({
      userId: 'EMP1001',
      passwordHash,
      status: UserStatus.SUSPENDED,
    });

    await expect(
      service.login({ user_id: 'EMP1001', password: 'Password@123' }),
    ).rejects.toThrow(UnauthorizedException);
  });

  it('should trigger rate limit after exceeding max attempts', async () => {
    userRepo.findOne.mockResolvedValue(null);

    for (let i = 0; i < 10; i++) {
      try {
        await service.login({ user_id: 'THROTTLED_USER', password: 'bad' }, '1.2.3.4');
      } catch {
        // expected
      }
    }

    await expect(
      service.login({ user_id: 'THROTTLED_USER', password: 'bad' }, '1.2.3.4'),
    ).rejects.toThrow(HttpException);
  });
});
