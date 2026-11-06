import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { getDatabaseConfig } from './config/database.config';
import { AuthModule } from './modules/auth/auth.module';
import { AppConfigModule } from './modules/config/config.module';
import { UsersModule } from './modules/users/users.module';
import { AttendanceModule } from './modules/attendance/attendance.module';
import { ModelsModule } from './modules/models/models.module';
import { AdminModule } from './modules/admin/admin.module';
import { TrackingModule } from './modules/tracking/tracking.module';
import { AppController } from './app.controller';
import { AppService } from './app.service';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TypeOrmModule.forRoot(getDatabaseConfig()),
    AuthModule,
    AppConfigModule,
    UsersModule,
    AttendanceModule,
    ModelsModule,
    AdminModule,
    TrackingModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
