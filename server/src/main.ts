import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import * as express from 'express';
import * as path from 'path';
import * as bcrypt from 'bcrypt';
import { DataSource } from 'typeorm';
import { AppModule } from './app.module';
import { HttpExceptionFilter } from './common/filters/http-exception.filter';
import {
  UserEntity,
  UserStatus,
  UserRole,
  OfficeConfigEntity,
} from './database/entities';

async function autoSeedIfEmpty(app: any) {
  try {
    const dataSource = app.get(DataSource);
    const officeRepo = dataSource.getRepository(OfficeConfigEntity);
    const userRepo = dataSource.getRepository(UserEntity);

    const officeCount = await officeRepo.count();
    if (officeCount === 0) {
      console.log('[AutoSeed] Initializing default office configuration...');
      const officeId = 'DEFAULT_OFFICE';
      const name = 'Headquarters';
      const latitude = 28.490293;
      const longitude = 77.060544;
      const radiusM = 100;
      const timezone = 'Asia/Kolkata';
      const matchThreshold = 0.46;
      const syncWindows = ['16:00', '00:00'];
      const syncJitterMinutes = 30;

      await officeRepo.save(
        officeRepo.create({
          officeId,
          name,
          latitude,
          longitude,
          radiusM,
          timezone,
          syncWindows,
          syncJitterMinutes,
          matchThreshold,
        }),
      );
    } else {
      // Ensure all existing offices are set to 0.46 threshold
      await officeRepo
        .createQueryBuilder()
        .update(OfficeConfigEntity)
        .set({ matchThreshold: 0.46 })
        .execute();
    }

    const userCount = await userRepo.count();
    if (userCount === 0) {
      console.log('[AutoSeed] Initializing default test accounts...');
      const defaultPasswordHash = await bcrypt.hash('Password@123', 10);
      const adminPasswordHash = await bcrypt.hash('Admin@123', 10);

      await userRepo.save([
        userRepo.create({
          userId: 'EMP1001',
          name: 'Alice Sharma',
          passwordHash: defaultPasswordHash,
          role: UserRole.EMPLOYEE,
          siteCode: 'DEFAULT_OFFICE',
          status: UserStatus.ACTIVE,
          photoChangeUsed: false,
        }),
        userRepo.create({
          userId: 'EMP1002',
          name: 'Bob Kumar',
          passwordHash: defaultPasswordHash,
          role: UserRole.EMPLOYEE,
          siteCode: 'DEFAULT_OFFICE',
          status: UserStatus.ACTIVE,
          photoChangeUsed: false,
        }),
        userRepo.create({
          userId: 'EMP1003',
          name: 'Charlie Singh',
          passwordHash: defaultPasswordHash,
          role: UserRole.EMPLOYEE,
          siteCode: 'DEFAULT_OFFICE',
          status: UserStatus.ACTIVE,
          photoChangeUsed: true,
        }),
        userRepo.create({
          userId: 'EMP2001',
          name: 'Manan',
          passwordHash: defaultPasswordHash,
          role: UserRole.ADMIN,
          siteCode: 'DEFAULT_OFFICE',
          status: UserStatus.ACTIVE,
          photoChangeUsed: false,
        }),
        userRepo.create({
          userId: 'ADMIN01',
          name: 'System Admin',
          passwordHash: adminPasswordHash,
          role: UserRole.ADMIN,
          siteCode: 'DEFAULT_OFFICE',
          status: UserStatus.ACTIVE,
          photoChangeUsed: false,
        }),
      ]);
      console.log('[AutoSeed] Initial accounts created (EMP1001, EMP1002, EMP1003, EMP2001, ADMIN01 / Password@123)');
    } else {
      // If admin accounts exist, ensure their role is ADMIN; if not, create ADMIN01
      const adminUser = await userRepo.findOne({ where: { userId: 'ADMIN01' } });
      if (adminUser) {
        await userRepo.update({ userId: 'ADMIN01' }, { role: UserRole.ADMIN });
      } else {
        const adminPasswordHash = await bcrypt.hash('Admin@123', 10);
        await userRepo.save(
          userRepo.create({
            userId: 'ADMIN01',
            name: 'System Admin',
            passwordHash: adminPasswordHash,
            role: UserRole.ADMIN,
            siteCode: 'DEFAULT_OFFICE',
            status: UserStatus.ACTIVE,
            photoChangeUsed: false,
          }),
        );
      }

      const mananUser = await userRepo.findOne({ where: { userId: 'EMP2001' } });
      if (mananUser) {
        await userRepo.update({ userId: 'EMP2001' }, { role: UserRole.ADMIN });
      }
    }
  } catch (err) {
    console.warn('[AutoSeed] Auto-seed skipped or encountered error:', err.message);
  }
}

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.enableCors();
  app.setGlobalPrefix('v1');
  app.useGlobalFilters(new HttpExceptionFilter());
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      transform: true,
      forbidNonWhitelisted: true,
    }),
  );

  // Serve uploaded files statically
  const uploadsDir = process.env.UPLOAD_DIR || 'uploads';
  const uploadsPath = path.resolve(process.cwd(), uploadsDir);
  app.use('/uploads', express.static(uploadsPath));

  const config = new DocumentBuilder()
    .setTitle('Attendance App Backend API')
    .setDescription('Face-verified geofenced attendance system API')
    .setVersion('1.0.0')
    .addBearerAuth()
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('docs', app, document);

  // Seed default configuration & users if empty
  await autoSeedIfEmpty(app);

  const port = process.env.PORT || 3000;
  await app.listen(port, '0.0.0.0');
  console.log(`Attendance Backend server running on http://0.0.0.0:${port}`);
  console.log(`Swagger API Docs available at http://localhost:${port}/docs`);
}
bootstrap();
