import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import {
  TrackingSessionEntity,
  TrackingPointEntity,
  TrackingStopEntity,
  UserEntity,
  UserSiteEntity,
  OfficeConfigEntity,
} from '../../database/entities';
import { TrackingService } from './tracking.service';
import { TrackingController } from './tracking.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      TrackingSessionEntity,
      TrackingPointEntity,
      TrackingStopEntity,
      UserEntity,
      UserSiteEntity,
      OfficeConfigEntity,
    ]),
  ],
  controllers: [TrackingController],
  providers: [TrackingService],
  exports: [TrackingService],
})
export class TrackingModule {}
