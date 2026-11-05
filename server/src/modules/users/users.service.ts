import {
  Injectable,
  BadRequestException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as crypto from 'crypto';
import { UserEntity } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import {
  PhotoUploadResponseDto,
  PhotoDetailResponseDto,
} from './dto/photo-response.dto';

function validateAndDetectImageType(file: Express.Multer.File): { mimeType: string } {
  if (!file || !file.buffer) {
    throw new BadRequestException({
      code: 'PHOTO_MISSING',
      message: 'No photo file provided in request.',
    });
  }

  const MAX_SIZE = 5 * 1024 * 1024; // 5 MB
  if (file.buffer.length > MAX_SIZE) {
    throw new BadRequestException({
      code: 'INVALID_IMAGE',
      message: 'Photo size exceeds maximum allowed size of 5 MB.',
    });
  }

  const buf = file.buffer;
  if (buf.length >= 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) {
    return { mimeType: 'image/jpeg' };
  }
  if (
    buf.length >= 8 &&
    buf[0] === 0x89 &&
    buf[1] === 0x50 &&
    buf[2] === 0x4e &&
    buf[3] === 0x47 &&
    buf[4] === 0x0d &&
    buf[5] === 0x0a &&
    buf[6] === 0x1a &&
    buf[7] === 0x0a
  ) {
    return { mimeType: 'image/png' };
  }

  throw new BadRequestException({
    code: 'INVALID_IMAGE',
    message: 'Invalid image format. Only JPEG and PNG images are allowed.',
  });
}

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(UserPhotoEntity)
    private readonly photoRepository: Repository<UserPhotoEntity>,
  ) {}

  async uploadPhotoV1(
    userId: string,
    file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    const { mimeType } = validateAndDetectImageType(file);

    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: 'User not found.',
      });
    }

    // Check existing photos for user
    const userPhotos = await this.photoRepository
      .createQueryBuilder('photo')
      .addSelect('photo.imageData')
      .where('photo.userId = :userId', { userId })
      .getMany();

    const existingV1WithData = userPhotos.find(
      (p) => p.version === 1 && p.imageData && p.imageData.length > 0,
    );
    if (existingV1WithData) {
      throw new BadRequestException({
        code: 'PHOTO_ALREADY_EXISTS',
        message: 'Initial profile photo (version 1) has already been uploaded.',
      });
    }

    // If legacy rows exist without image data, delete them and reset photo_change_used
    const legacyPhotos = userPhotos.filter(
      (p) => !p.imageData || p.imageData.length === 0,
    );
    if (legacyPhotos.length > 0) {
      await this.photoRepository.remove(legacyPhotos);
      user.photoChangeUsed = false;
      await this.userRepository.save(user);
    }

    // Compute sha256
    const hash = crypto.createHash('sha256').update(file.buffer).digest('hex');
    const photoUrl = '/v1/users/me/photo/file';

    const photoEntity = this.photoRepository.create({
      userId,
      version: 1,
      imagePath: photoUrl,
      imageData: file.buffer,
      mimeType,
      hash,
      isActive: true,
    });
    await this.photoRepository.save(photoEntity);

    return {
      version: 1,
      photo_url: photoUrl,
      photo_change_used: user.photoChangeUsed,
    };
  }

  async replacePhotoV2(
    userId: string,
    file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    const { mimeType } = validateAndDetectImageType(file);

    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: 'User not found.',
      });
    }

    // Strictly check if photo change is already used
    if (user.photoChangeUsed) {
      throw new ForbiddenException({
        code: 'PHOTO_CHANGE_EXHAUSTED',
        message:
          'Photo replacement already used. Only one photo change is allowed per user lifetime.',
      });
    }

    // Check if photo v1 with image data exists
    const existingV1 = await this.photoRepository
      .createQueryBuilder('photo')
      .addSelect('photo.imageData')
      .where('photo.userId = :userId AND photo.version = 1', { userId })
      .getOne();

    if (!existingV1 || !existingV1.imageData || existingV1.imageData.length === 0) {
      throw new BadRequestException({
        code: 'PHOTO_V1_MISSING',
        message: 'Cannot replace photo before initial photo (version 1) is uploaded.',
      });
    }

    // Compute sha256
    const hash = crypto.createHash('sha256').update(file.buffer).digest('hex');
    const photoUrl = '/v1/users/me/photo/file';

    // Deactivate version 1
    await this.photoRepository.update({ userId, version: 1 }, { isActive: false });

    // Save version 2
    const photoV2 = this.photoRepository.create({
      userId,
      version: 2,
      imagePath: photoUrl,
      imageData: file.buffer,
      mimeType,
      hash,
      isActive: true,
    });
    await this.photoRepository.save(photoV2);

    // Update user photo_change_used
    user.photoChangeUsed = true;
    await this.userRepository.save(user);

    return {
      version: 2,
      photo_url: photoUrl,
      photo_change_used: true,
    };
  }

  async getActivePhoto(userId: string): Promise<PhotoDetailResponseDto> {
    const photo = await this.photoRepository
      .createQueryBuilder('photo')
      .where('photo.userId = :userId', { userId })
      .andWhere('photo.isActive = :isActive', { isActive: true })
      .andWhere('photo.imageData IS NOT NULL')
      .orderBy('photo.version', 'DESC')
      .getOne();

    if (!photo) {
      throw new NotFoundException({
        code: 'PHOTO_NOT_FOUND',
        message: 'No active profile photo found for this user.',
      });
    }

    return {
      version: photo.version,
      photo_url: photo.imagePath,
      uploaded_at: photo.uploadedAt.toISOString(),
      hash: photo.hash,
    };
  }

  async getActivePhotoFile(
    userId: string,
  ): Promise<{ data: Buffer; mimeType: string; hash: string }> {
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
        message: 'No active profile photo found for this user.',
      });
    }

    return {
      data: photo.imageData,
      mimeType: photo.mimeType || 'image/jpeg',
      hash: photo.hash,
    };
  }
}
