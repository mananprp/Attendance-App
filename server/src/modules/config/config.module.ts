import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { UserEntity } from '../../database/entities/user.entity';
import { UserSiteEntity } from '../../database/entities/user-site.entity';
import { ConfigController } from './config.controller';
import { ConfigService } from './config.service';

@Module({
  imports: [TypeOrmModule.forFeature([OfficeConfigEntity, UserEntity, UserSiteEntity])],
  controllers: [ConfigController],
  providers: [ConfigService],
  exports: [ConfigService],
})
export class AppConfigModule {}
