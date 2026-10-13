import {
  Injectable,
  BadRequestException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as crypto from 'crypto';
import * as fs from 'fs';
import * as path from 'path';
import { UserEntity } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import {
  PhotoUploadResponseDto,
  PhotoDetailResponseDto,
} from './dto/photo-response.dto';

@Injectable()
export class UsersService {
  private readonly uploadDir = path.resolve(
    process.cwd(),
    process.env.UPLOAD_DIR || 'uploads',
    'photos',
  );

  constructor(
    @InjectRepository(UserEntity)
    private readonly userRepository: Repository<UserEntity>,
    @InjectRepository(UserPhotoEntity)
    private readonly photoRepository: Repository<UserPhotoEntity>,
  ) {
    if (!fs.existsSync(this.uploadDir)) {
      fs.mkdirSync(this.uploadDir, { recursive: true });
    }
  }

  async uploadPhotoV1(
    userId: string,
    file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    if (!file) {
      throw new BadRequestException({
        code: 'PHOTO_MISSING',
        message: 'No photo file provided in request.',
      });
    }

    const user = await this.userRepository.findOne({ where: { userId } });
    if (!user) {
      throw new NotFoundException({
        code: 'USER_NOT_FOUND',
        message: 'User not found.',
      });
    }

    // Check if photo v1 already exists
    const existingV1 = await this.photoRepository.findOne({
      where: { userId, version: 1 },
    });
    if (existingV1) {
      throw new BadRequestException({
        code: 'PHOTO_ALREADY_EXISTS',
        message: 'Initial profile photo (version 1) has already been uploaded.',
      });
    }

    // Compute sha256
    const hash = crypto.createHash('sha256').update(file.buffer).digest('hex');

    // Save file to disk
    const filename = `${userId}_v1.jpg`;
    const filePath = path.join(this.uploadDir, filename);
    fs.writeFileSync(filePath, file.buffer);

    const relativeUrl = `/uploads/photos/${filename}`;

    const photoEntity = this.photoRepository.create({
      userId,
      version: 1,
      imagePath: relativeUrl,
      hash,
      isActive: true,
    });
    await this.photoRepository.save(photoEntity);

    return {
      version: 1,
      photo_url: relativeUrl,
      photo_change_used: user.photoChangeUsed,
    };
  }

  async replacePhotoV2(
    userId: string,
    file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    if (!file) {
      throw new BadRequestException({
        code: 'PHOTO_MISSING',
        message: 'No photo file provided in request.',
      });
    }

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

    // Check if photo v1 exists
    const existingV1 = await this.photoRepository.findOne({
      where: { userId, version: 1 },
    });
    if (!existingV1) {
      throw new BadRequestException({
        code: 'PHOTO_V1_MISSING',
        message: 'Cannot replace photo before initial photo (version 1) is uploaded.',
      });
    }

    // Compute sha256
    const hash = crypto.createHash('sha256').update(file.buffer).digest('hex');

    // Save file to disk
    const filename = `${userId}_v2.jpg`;
    const filePath = path.join(this.uploadDir, filename);
    fs.writeFileSync(filePath, file.buffer);

    const relativeUrl = `/uploads/photos/${filename}`;

    // Deactivate version 1
    await this.photoRepository.update({ userId, version: 1 }, { isActive: false });

    // Save version 2
    const photoV2 = this.photoRepository.create({
      userId,
      version: 2,
      imagePath: relativeUrl,
      hash,
      isActive: true,
    });
    await this.photoRepository.save(photoV2);

    // Update user photo_change_used
    user.photoChangeUsed = true;
    await this.userRepository.save(user);

    return {
      version: 2,
      photo_url: relativeUrl,
      photo_change_used: true,
    };
  }

  async getActivePhoto(userId: string): Promise<PhotoDetailResponseDto> {
    const photo = await this.photoRepository.findOne({
      where: { userId, isActive: true },
      order: { version: 'DESC' },
    });

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
}
