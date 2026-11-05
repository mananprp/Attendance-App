import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { UserEntity } from '../../database/entities/user.entity';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { AttendancePunchEntity } from '../../database/entities/attendance-punch.entity';
import { UserPhotoEntity } from '../../database/entities/user-photo.entity';
import { AdminController } from './admin.controller';
import { AdminService } from './admin.service';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      UserEntity,
      OfficeConfigEntity,
      AttendancePunchEntity,
      UserPhotoEntity,
    ]),
  ],
  controllers: [AdminController],
  providers: [AdminService],
  exports: [AdminService],
})
export class AdminModule {}
