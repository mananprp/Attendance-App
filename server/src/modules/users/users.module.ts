import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '../../database/entities/user.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import { UserSiteEntity } from '../../database/entities/user-site.entity';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserEntity,
      UserPhotoEntity,
      UserSiteEntity,
      OfficeConfigEntity,
    ]),
  ],
  controllers: [UsersController],
  providers: [UsersService],
  exports: [UsersService],
})
export class UsersModule {}
