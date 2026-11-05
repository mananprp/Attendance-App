import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import * as crypto from 'crypto';
import { UsersService } from './users.service';
import { UserEntity } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';

describe('UsersService', () => {
  let service: UsersService;
  let userRepo: any;
  let photoRepo: any;

  const validJpegBuffer = Buffer.concat([
    Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46]),
    Buffer.from('sample_jpeg_data'),
  ]);

  const mockFile: Express.Multer.File = {
    fieldname: 'photo',
    originalname: 'test.jpg',
    encoding: '7bit',
    mimetype: 'image/jpeg',
    buffer: validJpegBuffer,
    size: validJpegBuffer.length,
    destination: '',
    filename: '',
    path: '',
    stream: null as any,
  };

  let mockQueryBuilder: any;

  beforeEach(async () => {
    mockQueryBuilder = {
      addSelect: jest.fn().mockReturnThis(),
      where: jest.fn().mockReturnThis(),
      andWhere: jest.fn().mockReturnThis(),
      orderBy: jest.fn().mockReturnThis(),
      getOne: jest.fn(),
      getMany: jest.fn().mockResolvedValue([]),
      delete: jest.fn().mockReturnThis(),
      from: jest.fn().mockReturnThis(),
      execute: jest.fn().mockResolvedValue({ affected: 1 }),
    };

    userRepo = {
      findOne: jest.fn(),
      save: jest.fn((u) => Promise.resolve(u)),
    };
    photoRepo = {
      findOne: jest.fn(),
      createQueryBuilder: jest.fn(() => mockQueryBuilder),
      create: jest.fn((dto) => dto),
      save: jest.fn((entity) => Promise.resolve(entity)),
      update: jest.fn().mockResolvedValue({ affected: 1 }),
      remove: jest.fn().mockResolvedValue([]),
      delete: jest.fn().mockResolvedValue({ affected: 1 }),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        {
          provide: getRepositoryToken(UserEntity),
          useValue: userRepo,
        },
        {
          provide: getRepositoryToken(UserPhotoEntity),
          useValue: photoRepo,
        },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
  });

  describe('uploadPhotoV1', () => {
    it('should successfully upload photo v1 to database without writing files', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      mockQueryBuilder.getMany.mockResolvedValue([]);

      const result = await service.uploadPhotoV1('EMP1001', mockFile);
      expect(result.version).toBe(1);
      expect(result.photo_url).toBe('/v1/users/me/photo/file');
      expect(result.photo_change_used).toBe(false);

      expect(photoRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({
          userId: 'EMP1001',
          version: 1,
          imagePath: '/v1/users/me/photo/file',
          imageData: validJpegBuffer,
          mimeType: 'image/jpeg',
          isActive: true,
        }),
      );
    });

    it('should throw BadRequestException if photo v1 already exists with image data', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      mockQueryBuilder.getMany.mockResolvedValue([
        {
          id: 1,
          userId: 'EMP1001',
          version: 1,
          imageData: Buffer.from('existing_data'),
        },
      ]);

      await expect(service.uploadPhotoV1('EMP1001', mockFile)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should allow re-enrolment if existing v1 row has NO image data (legacy row)', async () => {
      const user = { userId: 'EMP1001', photoChangeUsed: true };
      userRepo.findOne.mockResolvedValue(user);
      const legacyRow = {
        id: 99,
        userId: 'EMP1001',
        version: 1,
        imageData: null,
      };
      mockQueryBuilder.getMany.mockResolvedValue([legacyRow]);

      const result = await service.uploadPhotoV1('EMP1001', mockFile);
      expect(result.version).toBe(1);
      expect(result.photo_url).toBe('/v1/users/me/photo/file');
      expect(photoRepo.remove).toHaveBeenCalledWith([legacyRow]);
      expect(user.photoChangeUsed).toBe(false);
      expect(userRepo.save).toHaveBeenCalledWith(user);
    });

    it('should throw BadRequestException if no file is provided', async () => {
      await expect(service.uploadPhotoV1('EMP1001', null as any)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should reject invalid magic bytes with 400', async () => {
      const badFile = {
        ...mockFile,
        buffer: Buffer.from('not_a_real_image_file'),
        size: 21,
      };
      await expect(service.uploadPhotoV1('EMP1001', badFile)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should reject files larger than 5 MB with 400', async () => {
      const hugeBuffer = Buffer.alloc(6 * 1024 * 1024);
      hugeBuffer[0] = 0xff;
      hugeBuffer[1] = 0xd8;
      hugeBuffer[2] = 0xff;
      const hugeFile = {
        ...mockFile,
        buffer: hugeBuffer,
        size: hugeBuffer.length,
      };
      await expect(service.uploadPhotoV1('EMP1001', hugeFile)).rejects.toThrow(
        BadRequestException,
      );
    });
  });

  describe('replacePhotoV2', () => {
    it('should successfully replace photo with v2 and mark photoChangeUsed', async () => {
      const user = { userId: 'EMP1001', photoChangeUsed: false };
      userRepo.findOne.mockResolvedValue(user);
      mockQueryBuilder.getOne.mockResolvedValue({
        id: 1,
        userId: 'EMP1001',
        version: 1,
        imageData: validJpegBuffer,
        isActive: true,
      });

      const result = await service.replacePhotoV2('EMP1001', mockFile);
      expect(result.version).toBe(2);
      expect(result.photo_url).toBe('/v1/users/me/photo/file');
      expect(result.photo_change_used).toBe(true);
      expect(photoRepo.update).toHaveBeenCalledWith(
        { userId: 'EMP1001', version: 1 },
        { isActive: false },
      );
      expect(photoRepo.save).toHaveBeenCalledWith(
        expect.objectContaining({
          userId: 'EMP1001',
          version: 2,
          imagePath: '/v1/users/me/photo/file',
          imageData: validJpegBuffer,
          isActive: true,
        }),
      );
      expect(user.photoChangeUsed).toBe(true);
    });

    it('should strictly reject if photoChangeUsed is true with 403 ForbiddenException', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: true });

      await expect(service.replacePhotoV2('EMP1001', mockFile)).rejects.toThrow(
        ForbiddenException,
      );
    });

    it('should throw BadRequestException if photo v1 does not exist yet', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      mockQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.replacePhotoV2('EMP1001', mockFile)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should throw BadRequestException if photo v1 exists but has no image data', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      mockQueryBuilder.getOne.mockResolvedValue({
        id: 1,
        userId: 'EMP1001',
        version: 1,
        imageData: null,
      });

      await expect(service.replacePhotoV2('EMP1001', mockFile)).rejects.toThrow(
        BadRequestException,
      );
    });
  });

  describe('getActivePhoto', () => {
    it('should return active photo details when photo with image data exists', async () => {
      mockQueryBuilder.getOne.mockResolvedValue({
        version: 1,
        imagePath: '/v1/users/me/photo/file',
        uploadedAt: new Date('2026-09-30T09:00:00.000Z'),
        hash: 'mockhash',
      });

      const result = await service.getActivePhoto('EMP1001');
      expect(result.version).toBe(1);
      expect(result.photo_url).toBe('/v1/users/me/photo/file');
      expect(result.hash).toBe('mockhash');
    });

    it('should throw NotFoundException if no active photo with image data exists', async () => {
      mockQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.getActivePhoto('EMP1001')).rejects.toThrow(
        NotFoundException,
      );
    });
  });

  describe('getActivePhotoFile', () => {
    it('should return raw bytes, mimeType, and hash for active photo', async () => {
      const hash = crypto.createHash('sha256').update(validJpegBuffer).digest('hex');
      mockQueryBuilder.getOne.mockResolvedValue({
        version: 1,
        imageData: validJpegBuffer,
        mimeType: 'image/jpeg',
        hash,
      });

      const result = await service.getActivePhotoFile('EMP1001');
      expect(result.data).toEqual(validJpegBuffer);
      expect(result.mimeType).toBe('image/jpeg');
      expect(result.hash).toBe(hash);
    });

    it('should throw NotFoundException if no photo file exists', async () => {
      mockQueryBuilder.getOne.mockResolvedValue(null);

      await expect(service.getActivePhotoFile('EMP1001')).rejects.toThrow(
        NotFoundException,
      );
    });
  });
});
