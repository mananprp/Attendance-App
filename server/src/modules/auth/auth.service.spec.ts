import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { JwtService } from '@nestjs/jwt';
import { UnauthorizedException } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import { AuthService } from './auth.service';
import { UserEntity, UserStatus } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';

describe('AuthService', () => {
  let service: AuthService;
  let userRepo: any;
  let photoRepo: any;
  let jwtService: any;
  let mockQueryBuilder: any;

  beforeEach(async () => {
    mockQueryBuilder = {
      where: jest.fn().mockReturnThis(),
      andWhere: jest.fn().mockReturnThis(),
      orderBy: jest.fn().mockReturnThis(),
      getOne: jest.fn(),
    };

    userRepo = {
      findOne: jest.fn(),
    };
    photoRepo = {
      findOne: jest.fn(),
      createQueryBuilder: jest.fn(() => mockQueryBuilder),
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

  it('should successfully login with valid credentials and active photo in database', async () => {
    const passwordHash = await bcrypt.hash('Password@123', 10);
    const mockUser: Partial<UserEntity> = {
      userId: 'EMP1001',
      name: 'Alice Sharma',
      passwordHash,
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    };
    userRepo.findOne.mockResolvedValue(mockUser);
    mockQueryBuilder.getOne.mockResolvedValue({ version: 1, isActive: true, imageData: Buffer.from('data') });

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

  it('should return has_photo = false and active_photo_version = null when legacy row has no image data', async () => {
    const passwordHash = await bcrypt.hash('Password@123', 10);
    const mockUser: Partial<UserEntity> = {
      userId: 'EMP1001',
      name: 'Alice Sharma',
      passwordHash,
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    };
    userRepo.findOne.mockResolvedValue(mockUser);
    // Query builder filters by photo.imageData IS NOT NULL, so legacy row yields null
    mockQueryBuilder.getOne.mockResolvedValue(null);

    const result = await service.login({
      user_id: 'EMP1001',
      password: 'Password@123',
    });

    expect(result.user.has_photo).toBe(false);
    expect(result.user.active_photo_version).toBeNull();
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
});
