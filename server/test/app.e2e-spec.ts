process.env.NODE_ENV = 'test';
process.env.DB_TYPE = 'sqlite';

import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import * as request from 'supertest';
import * as bcrypt from 'bcryptjs';
import { DataSource } from 'typeorm';
import { AppModule } from '../src/app.module';
import { HttpExceptionFilter } from '../src/common/filters/http-exception.filter';
import {
  UserEntity,
  UserStatus,
  UserRole,
  UserPhotoEntity,
  OfficeConfigEntity,
  AttendancePunchEntity,
  AttendanceDayStatus,
} from '../src/database/entities';

describe('Attendance App End-to-End Tests', () => {
  let app: INestApplication;
  let dataSource: DataSource;
  let authTokenEmp1001: string;
  let authTokenEmp1002: string;
  let authTokenEmp1003: string;
  let authTokenAdmin: string;

  const validJpegBuffer1 = Buffer.concat([
    Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46]),
    Buffer.from('fake_image_bytes_v1'),
  ]);

  const validJpegBuffer2 = Buffer.concat([
    Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46]),
    Buffer.from('fake_image_bytes_v2'),
  ]);

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.setGlobalPrefix('v1');
    app.useGlobalFilters(new HttpExceptionFilter());
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        transform: true,
        forbidNonWhitelisted: true,
      }),
    );

    await app.init();
    dataSource = app.get(DataSource);

    // Seed database with office config and test users
    const officeRepo = dataSource.getRepository(OfficeConfigEntity);
    const userRepo = dataSource.getRepository(UserEntity);

    await officeRepo.save(
      officeRepo.create({
        officeId: 'DEFAULT_OFFICE',
        name: 'Headquarters',
        latitude: 28.613939,
        longitude: 77.209021,
        radiusM: 100,
        timezone: 'Asia/Kolkata',
        syncWindows: ['16:00', '00:00'],
        syncJitterMinutes: 30,
        matchThreshold: 0.55,
      }),
    );

    const passwordHash = await bcrypt.hash('Password@123', 10);

    await userRepo.save([
      userRepo.create({
        userId: 'EMP1001',
        name: 'Alice Sharma',
        passwordHash,
        role: UserRole.EMPLOYEE,
        status: UserStatus.ACTIVE,
        photoChangeUsed: false,
      }),
      userRepo.create({
        userId: 'EMP1002',
        name: 'Bob Kumar',
        passwordHash,
        role: UserRole.EMPLOYEE,
        status: UserStatus.ACTIVE,
        photoChangeUsed: false,
      }),
      userRepo.create({
        userId: 'EMP1003',
        name: 'Charlie Singh',
        passwordHash,
        role: UserRole.EMPLOYEE,
        status: UserStatus.ACTIVE,
        photoChangeUsed: true,
      }),
      userRepo.create({
        userId: 'ADMIN001',
        name: 'Admin Boss',
        passwordHash,
        role: UserRole.ADMIN,
        status: UserStatus.ACTIVE,
        photoChangeUsed: false,
      }),
      userRepo.create({
        userId: 'EMP_SUSPENDED',
        name: 'Suspended User',
        passwordHash,
        role: UserRole.EMPLOYEE,
        status: UserStatus.SUSPENDED,
        photoChangeUsed: false,
      }),
    ]);
  });

  afterAll(async () => {
    await app.close();
  });

  describe('Health Endpoint', () => {
    it('GET /v1/health should return 200 OK', () => {
      return request(app.getHttpServer())
        .get('/v1/health')
        .expect(200)
        .expect((res) => {
          expect(res.body.status).toBe('ok');
          expect(res.body.timestamp).toBeDefined();
        });
    });
  });

  describe('Auth Flow (POST /v1/auth/login)', () => {
    it('should successfully log in EMP1001 with valid credentials', async () => {
      const res = await request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP1001', password: 'Password@123' })
        .expect(200);

      expect(res.body.token).toBeDefined();
      expect(res.body.user).toBeDefined();
      expect(res.body.user.user_id).toBe('EMP1001');
      expect(res.body.user.display_name).toBe('Alice Sharma');
      expect(res.body.user.photo_change_used).toBe(false);
      expect(res.body.user.has_photo).toBe(false);
      authTokenEmp1001 = res.body.token;
    });

    it('should successfully log in EMP1002', async () => {
      const res = await request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP1002', password: 'Password@123' })
        .expect(200);

      authTokenEmp1002 = res.body.token;
    });

    it('should successfully log in EMP1003 (with photo_change_used = true)', async () => {
      const res = await request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP1003', password: 'Password@123' })
        .expect(200);

      expect(res.body.user.photo_change_used).toBe(true);
      authTokenEmp1003 = res.body.token;
    });

    it('should successfully log in ADMIN001', async () => {
      const res = await request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'ADMIN001', password: 'Password@123' })
        .expect(200);

      expect(res.body.user.role).toBe('ADMIN');
      authTokenAdmin = res.body.token;
    });

    it('should return 401 Unauthorized for incorrect password', () => {
      return request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP1001', password: 'WrongPassword' })
        .expect(401)
        .expect((res) => {
          expect(res.body.code).toBe('INVALID_CREDENTIALS');
        });
    });

    it('should return 401 Unauthorized for non-existent user', () => {
      return request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'UNKNOWN_USER', password: 'Password@123' })
        .expect(401)
        .expect((res) => {
          expect(res.body.code).toBe('INVALID_CREDENTIALS');
        });
    });

    it('should return 401 Unauthorized for suspended user', () => {
      return request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP_SUSPENDED', password: 'Password@123' })
        .expect(401)
        .expect((res) => {
          expect(res.body.code).toBe('ACCOUNT_INACTIVE');
        });
    });
  });

  describe('Config Endpoint (GET /v1/config)', () => {
    it('should return 401 Unauthorized when no token provided', () => {
      return request(app.getHttpServer()).get('/v1/config').expect(401);
    });

    it('should return office configuration with valid token', () => {
      return request(app.getHttpServer())
        .get('/v1/config')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(200)
        .expect((res) => {
          expect(res.body.office_lat).toBe(28.613939);
          expect(res.body.office_lng).toBe(77.209021);
          expect(res.body.radius_m).toBe(100);
          expect(res.body.timezone).toBe('Asia/Kolkata');
          expect(res.body.sync_windows).toEqual(['16:00', '00:00']);
          expect(res.body.sync_jitter_minutes).toBe(30);
          expect(res.body.match_threshold).toBe(0.55);
        });
    });
  });

  describe('Models Manifest Endpoint (GET /v1/models/manifest)', () => {
    it('should return model manifest with 3 ONNX models (public endpoint)', () => {
      return request(app.getHttpServer())
        .get('/v1/models/manifest')
        .expect(200)
        .expect((res) => {
          expect(res.body.models).toBeDefined();
          expect(res.body.models.length).toBe(3);
          const names = res.body.models.map((m: any) => m.name);
          expect(names).toContain('det_10g');
          expect(names).toContain('1k3d68');
          expect(names).toContain('w600k_r50');
          expect(res.body.total_size_bytes).toBeGreaterThan(0);
        });
    });
  });

  describe('Photo Management Flow', () => {
    let photoEtag: string;

    it('GET /v1/users/me/photo should return 404 when no photo exists', () => {
      return request(app.getHttpServer())
        .get('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(404)
        .expect((res) => {
          expect(res.body.code).toBe('PHOTO_NOT_FOUND');
        });
    });

    it('POST /v1/users/me/photo should upload photo v1 successfully to DB', async () => {
      const res = await request(app.getHttpServer())
        .post('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .attach('photo', validJpegBuffer1, 'photo_v1.jpg')
        .expect(201);

      expect(res.body.version).toBe(1);
      expect(res.body.photo_url).toBe('/v1/users/me/photo/file');
      expect(res.body.photo_change_used).toBe(false);
    });

    it('POST /v1/users/me/photo should reject duplicate v1 upload with 400 Bad Request', () => {
      return request(app.getHttpServer())
        .post('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .attach('photo', validJpegBuffer1, 'photo_v1.jpg')
        .expect(400)
        .expect((res) => {
          expect(res.body.code).toBe('PHOTO_ALREADY_EXISTS');
        });
    });

    it('GET /v1/users/me/photo should return active photo v1 details', async () => {
      const res = await request(app.getHttpServer())
        .get('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(200);

      expect(res.body.version).toBe(1);
      expect(res.body.photo_url).toBe('/v1/users/me/photo/file');
      expect(res.body.uploaded_at).toBeDefined();
      expect(res.body.hash).toBeDefined();
    });

    it('GET /v1/users/me/photo/file should return active photo bytes and set ETag', async () => {
      const res = await request(app.getHttpServer())
        .get('/v1/users/me/photo/file')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(200);

      expect(res.headers['content-type']).toBe('image/jpeg');
      expect(res.headers['etag']).toBeDefined();
      expect(res.headers['cache-control']).toBe('private, max-age=0, must-revalidate');
      expect(res.body).toEqual(validJpegBuffer1);
      photoEtag = res.headers['etag'];
    });

    it('GET /v1/users/me/photo/file with If-None-Match should return 304 Not Modified', () => {
      return request(app.getHttpServer())
        .get('/v1/users/me/photo/file')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .set('If-None-Match', photoEtag)
        .expect(304);
    });

    it('PUT /v1/users/me/photo should successfully replace photo with v2', async () => {
      const res = await request(app.getHttpServer())
        .put('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .attach('photo', validJpegBuffer2, 'photo_v2.jpg')
        .expect(200);

      expect(res.body.version).toBe(2);
      expect(res.body.photo_url).toBe('/v1/users/me/photo/file');
      expect(res.body.photo_change_used).toBe(true);
    });

    it('GET /v1/users/me/photo should now return active photo v2', async () => {
      const res = await request(app.getHttpServer())
        .get('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(200);

      expect(res.body.version).toBe(2);
      expect(res.body.photo_url).toBe('/v1/users/me/photo/file');
    });

    it('PUT /v1/users/me/photo should strictly reject 2nd photo change with 403 Forbidden', () => {
      return request(app.getHttpServer())
        .put('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .attach('photo', validJpegBuffer1, 'photo_v3.jpg')
        .expect(403)
        .expect((res) => {
          expect(res.body.code).toBe('PHOTO_CHANGE_EXHAUSTED');
        });
    });

    it('PUT /v1/users/me/photo on EMP1003 (photo_change_used=true) should also be rejected with 403 Forbidden', () => {
      return request(app.getHttpServer())
        .put('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1003}`)
        .attach('photo', validJpegBuffer1, 'photo.jpg')
        .expect(403)
        .expect((res) => {
          expect(res.body.code).toBe('PHOTO_CHANGE_EXHAUSTED');
        });
    });

    it('PUT /v1/users/me/photo on EMP1002 (no photo v1 uploaded yet) should reject with 400 Bad Request', () => {
      return request(app.getHttpServer())
        .put('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1002}`)
        .attach('photo', validJpegBuffer1, 'photo.jpg')
        .expect(400)
        .expect((res) => {
          expect(res.body.code).toBe('PHOTO_V1_MISSING');
        });
    });

    it('Legacy row (null image_data) returns 404 and allows re-enrolment without PHOTO_ALREADY_EXISTS', async () => {
      const photoRepo = dataSource.getRepository(UserPhotoEntity);
      // Create a legacy row for EMP1002 with null image_data
      await photoRepo.save(
        photoRepo.create({
          userId: 'EMP1002',
          version: 1,
          imagePath: '/uploads/photos/EMP1002_v1.jpg',
          hash: 'legacyhash',
          imageData: null,
          isActive: true,
        }),
      );

      // GET /v1/users/me/photo should return 404 for legacy row
      await request(app.getHttpServer())
        .get('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1002}`)
        .expect(404);

      // Login should report has_photo = false for legacy row
      const loginRes = await request(app.getHttpServer())
        .post('/v1/auth/login')
        .send({ user_id: 'EMP1002', password: 'Password@123' })
        .expect(200);
      expect(loginRes.body.user.has_photo).toBe(false);

      // Upload v1 must succeed and delete legacy row
      const uploadRes = await request(app.getHttpServer())
        .post('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1002}`)
        .attach('photo', validJpegBuffer1, 'new_v1.jpg')
        .expect(201);

      expect(uploadRes.body.version).toBe(1);
      expect(uploadRes.body.photo_url).toBe('/v1/users/me/photo/file');

      // Now GET /v1/users/me/photo succeeds
      await request(app.getHttpServer())
        .get('/v1/users/me/photo')
        .set('Authorization', `Bearer ${authTokenEmp1002}`)
        .expect(200);
    });

    describe('Admin Reset Photo Flow', () => {
      it('POST /v1/admin/users/:userId/photo/reset returns 403 when non-admin calls it', () => {
        return request(app.getHttpServer())
          .post('/v1/admin/users/EMP1001/photo/reset')
          .set('Authorization', `Bearer ${authTokenEmp1001}`)
          .expect(403);
      });

      it('POST /v1/admin/users/:userId/photo/reset returns 404 for non-existent user', () => {
        return request(app.getHttpServer())
          .post('/v1/admin/users/NON_EXISTENT_999/photo/reset')
          .set('Authorization', `Bearer ${authTokenAdmin}`)
          .expect(404);
      });

      it('POST /v1/admin/users/:userId/photo/reset resets photos and allows re-enrolment', async () => {
        const res = await request(app.getHttpServer())
          .post('/v1/admin/users/EMP1001/photo/reset')
          .set('Authorization', `Bearer ${authTokenAdmin}`)
          .expect(201);

        expect(res.body.success).toBe(true);

        // Active photo metadata now returns 404
        await request(app.getHttpServer())
          .get('/v1/users/me/photo')
          .set('Authorization', `Bearer ${authTokenEmp1001}`)
          .expect(404);

        // Photo file endpoint returns 404
        await request(app.getHttpServer())
          .get('/v1/users/me/photo/file')
          .set('Authorization', `Bearer ${authTokenEmp1001}`)
          .expect(404);

        // Login now reports has_photo = false, photo_change_used = false
        const loginRes = await request(app.getHttpServer())
          .post('/v1/auth/login')
          .send({ user_id: 'EMP1001', password: 'Password@123' })
          .expect(200);
        expect(loginRes.body.user.has_photo).toBe(false);
        expect(loginRes.body.user.photo_change_used).toBe(false);

        // User can now re-enrol v1
        await request(app.getHttpServer())
          .post('/v1/users/me/photo')
          .set('Authorization', `Bearer ${authTokenEmp1001}`)
          .attach('photo', validJpegBuffer1, 're_enrolled.jpg')
          .expect(201);
      });
    });
  });

  describe('Attendance Sync and History Flow', () => {
    const punchUuid1 = '550e8400-e29b-41d4-a716-446655440011';
    const punchUuid2 = '550e8400-e29b-41d4-a716-446655440012';
    const punchUuid3 = '550e8400-e29b-41d4-a716-446655440013';

    it('POST /v1/attendance/sync should process batch and store image_path as null (ignoring image files)', async () => {
      const batch = [
        {
          punch_uuid: punchUuid1,
          user_id: 'EMP1001',
          date_local: '2026-09-30',
          type: 'IN',
          punch_time_utc: '2026-09-30T03:45:00.000Z',
          time_source: 'NTP',
          time_unverified: false,
          latitude: 28.61394,
          longitude: 77.209025,
          distance_m: 12.4,
          match_score: 0.8842,
          image_file_name: `${punchUuid1}.jpg`,
        },
        {
          punch_uuid: punchUuid2,
          user_id: 'EMP1001',
          date_local: '2026-09-30',
          type: 'OUT',
          punch_time_utc: '2026-09-30T12:30:00.000Z',
          time_source: 'NTP',
          time_unverified: false,
          latitude: 28.61394,
          longitude: 77.209025,
          distance_m: 15.0,
          match_score: 0.912,
          image_file_name: `${punchUuid2}.jpg`,
        },
      ];

      const res = await request(app.getHttpServer())
        .post('/v1/attendance/sync')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .field('batch_json', JSON.stringify(batch))
        .attach('images', Buffer.from('punch_image_1'), `${punchUuid1}.jpg`)
        .attach('images', Buffer.from('punch_image_2'), `${punchUuid2}.jpg`)
        .expect(200);

      expect(res.body.batch_id).toBeDefined();
      expect(res.body.processed_count).toBe(2);
      expect(res.body.results).toHaveLength(2);
      expect(res.body.results[0].status).toBe('ACCEPTED');
      expect(res.body.results[1].status).toBe('ACCEPTED');

      // Verify in DB that imagePath is null
      const punchRepo = dataSource.getRepository(AttendancePunchEntity);
      const savedPunch = await punchRepo.findOne({ where: { punchUuid: punchUuid1 } });
      expect(savedPunch).toBeDefined();
      expect(savedPunch?.imagePath).toBeNull();
    });

    it('POST /v1/attendance/sync should be idempotent and return DUPLICATE for existing punch_uuid', async () => {
      const batch = [
        {
          punch_uuid: punchUuid1,
          user_id: 'EMP1001',
          date_local: '2026-09-30',
          type: 'IN',
          punch_time_utc: '2026-09-30T03:45:00.000Z',
          time_source: 'NTP',
          time_unverified: false,
          latitude: 28.61394,
          longitude: 77.209025,
          distance_m: 12.4,
          match_score: 0.8842,
        },
      ];

      const res = await request(app.getHttpServer())
        .post('/v1/attendance/sync')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .field('batch_json', JSON.stringify(batch))
        .expect(200);

      expect(res.body.results[0].punch_uuid).toBe(punchUuid1);
      expect(res.body.results[0].status).toBe('DUPLICATE');
    });

    it('POST /v1/attendance/sync single IN punch on next day should mark day as MISSED_PUNCH', async () => {
      const batch = [
        {
          punch_uuid: punchUuid3,
          user_id: 'EMP1001',
          date_local: '2026-09-29',
          type: 'IN',
          punch_time_utc: '2026-09-29T03:30:00.000Z',
          time_source: 'DEVICE',
          time_unverified: true,
          latitude: 28.61394,
          longitude: 77.209025,
          distance_m: 10.0,
          match_score: 0.85,
        },
      ];

      const res = await request(app.getHttpServer())
        .post('/v1/attendance/sync')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .field('batch_json', JSON.stringify(batch))
        .expect(200);

      expect(res.body.results[0].status).toBe('ACCEPTED');
    });

    it('GET /v1/attendance?month=2026-09 should return days summary and punches', async () => {
      const res = await request(app.getHttpServer())
        .get('/v1/attendance?month=2026-09')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(200);

      expect(res.body.month).toBe('2026-09');
      expect(res.body.days).toHaveLength(2);

      // Check COMPLETE day
      const day30 = res.body.days.find((d: any) => d.date_local === '2026-09-30');
      expect(day30).toBeDefined();
      expect(day30.status).toBe(AttendanceDayStatus.COMPLETE);
      expect(day30.in_time).toBeDefined();
      expect(day30.out_time).toBeDefined();

      // Check MISSED_PUNCH day with time_unverified
      const day29 = res.body.days.find((d: any) => d.date_local === '2026-09-29');
      expect(day29).toBeDefined();
      expect(day29.status).toBe(AttendanceDayStatus.MISSED_PUNCH);
      expect(day29.time_unverified).toBe(true);

      // Check punches list
      expect(res.body.punches).toHaveLength(3);
    });

    it('GET /v1/attendance with invalid month format should return 400 Bad Request', () => {
      return request(app.getHttpServer())
        .get('/v1/attendance?month=2026-13')
        .set('Authorization', `Bearer ${authTokenEmp1001}`)
        .expect(400)
        .expect((res) => {
          expect(res.body.code).toBe('INVALID_MONTH_FORMAT');
        });
    });
  });
});
