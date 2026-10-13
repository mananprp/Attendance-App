import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { UsersService } from './users.service';
import { UserEntity } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';

describe('UsersService', () => {
  let service: UsersService;
  let userRepo: any;
  let photoRepo: any;

  const mockFile: Express.Multer.File = {
    fieldname: 'photo',
    originalname: 'test.jpg',
    encoding: '7bit',
    mimetype: 'image/jpeg',
    buffer: Buffer.from('mock_image_bytes'),
    size: 16,
    destination: '',
    filename: '',
    path: '',
    stream: null as any,
  };

  beforeEach(async () => {
    userRepo = {
      findOne: jest.fn(),
      save: jest.fn(),
    };
    photoRepo = {
      findOne: jest.fn(),
      create: jest.fn((dto) => dto),
      save: jest.fn((entity) => Promise.resolve(entity)),
      update: jest.fn(),
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
    it('should successfully upload photo v1', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      photoRepo.findOne.mockResolvedValue(null);

      const result = await service.uploadPhotoV1('EMP1001', mockFile);
      expect(result.version).toBe(1);
      expect(result.photo_url).toContain('EMP1001_v1.jpg');
      expect(result.photo_change_used).toBe(false);
      expect(photoRepo.save).toHaveBeenCalled();
    });

    it('should throw BadRequestException if photo v1 already uploaded', async () => {
      userRepo.findOne.mockResolvedValue({ userId: 'EMP1001', photoChangeUsed: false });
      photoRepo.findOne.mockResolvedValue({ version: 1, userId: 'EMP1001' });

      await expect(service.uploadPhotoV1('EMP1001', mockFile)).rejects.toThrow(
        BadRequestException,
      );
    });

    it('should throw BadRequestException if no file is provided', async () => {
      await expect(service.uploadPhotoV1('EMP1001', null as any)).rejects.toThrow(
        BadRequestException,
      );
    });
  });

  describe('replacePhotoV2', () => {
    it('should successfully replace photo with v2 and mark photoChangeUsed', async () => {
      const user = { userId: 'EMP1001', photoChangeUsed: false };
      userRepo.findOne.mockResolvedValue(user);
      photoRepo.findOne.mockResolvedValue({ version: 1, userId: 'EMP1001', isActive: true });

      const result = await service.replacePhotoV2('EMP1001', mockFile);
      expect(result.version).toBe(2);
      expect(result.photo_url).toContain('EMP1001_v2.jpg');
      expect(result.photo_change_used).toBe(true);
      expect(photoRepo.update).toHaveBeenCalledWith(
        { userId: 'EMP1001', version: 1 },
        { isActive: false },
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
      photoRepo.findOne.mockResolvedValue(null);

      await expect(service.replacePhotoV2('EMP1001', mockFile)).rejects.toThrow(
        BadRequestException,
      );
    });
  });

  describe('getActivePhoto', () => {
    it('should return active photo details', async () => {
      photoRepo.findOne.mockResolvedValue({
        version: 1,
        imagePath: '/uploads/photos/EMP1001_v1.jpg',
        uploadedAt: new Date('2026-09-30T09:00:00.000Z'),
        hash: 'mockhash',
      });

      const result = await service.getActivePhoto('EMP1001');
      expect(result.version).toBe(1);
      expect(result.photo_url).toBe('/uploads/photos/EMP1001_v1.jpg');
      expect(result.hash).toBe('mockhash');
    });

    it('should throw NotFoundException if no active photo', async () => {
      photoRepo.findOne.mockResolvedValue(null);

      await expect(service.getActivePhoto('EMP1001')).rejects.toThrow(
        NotFoundException,
      );
    });
  });
});
