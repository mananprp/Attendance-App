import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { ConfigService } from './config.service';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';
import { UserEntity } from '../../database/entities/user.entity';

describe('ConfigService', () => {
  let service: ConfigService;
  let officeRepo: any;
  let userRepo: any;

  beforeEach(async () => {
    officeRepo = {
      findOne: jest.fn(),
    };
    userRepo = {
      findOne: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ConfigService,
        {
          provide: getRepositoryToken(OfficeConfigEntity),
          useValue: officeRepo,
        },
        {
          provide: getRepositoryToken(UserEntity),
          useValue: userRepo,
        },
      ],
    }).compile();

    service = module.get<ConfigService>(ConfigService);
  });

  it('should return office config from database when found', async () => {
    officeRepo.findOne.mockResolvedValue({
      officeId: 'DEFAULT_OFFICE',
      name: 'Headquarters',
      latitude: '28.613939',
      longitude: '77.209021',
      radiusM: 100,
      timezone: 'Asia/Kolkata',
      syncWindows: ['16:00', '00:00'],
      syncJitterMinutes: 30,
      matchThreshold: '0.5500',
    });

    const result = await service.getAppConfig();
    expect(result.office_lat).toBe(28.613939);
    expect(result.office_lng).toBe(77.209021);
    expect(result.radius_m).toBe(100);
    expect(result.timezone).toBe('Asia/Kolkata');
    expect(result.sync_windows).toEqual(['16:00', '00:00']);
  });

  it('should return fallback defaults when not found in database', async () => {
    officeRepo.findOne.mockResolvedValue(null);

    const result = await service.getAppConfig();
    expect(result.office_lat).toBe(28.490293);
    expect(result.office_lng).toBe(77.060544);
    expect(result.radius_m).toBe(100);
    expect(result.timezone).toBe('Asia/Kolkata');
  });

  it('should return site config assigned to user', async () => {
    userRepo.findOne.mockResolvedValue({ siteCode: 'SITE_REMOTE' });
    officeRepo.findOne.mockResolvedValue({
      officeId: 'SITE_REMOTE',
      name: 'Remote Site',
      latitude: '28.500000',
      longitude: '77.100000',
      radiusM: 200,
      timezone: 'Asia/Kolkata',
      syncWindows: ['18:00'],
      syncJitterMinutes: 15,
      matchThreshold: '0.46',
    });

    const result = await service.getAppConfig('EMP1001');
    expect(result.office_lat).toBe(28.5);
    expect(result.radius_m).toBe(200);
    expect(userRepo.findOne).toHaveBeenCalledWith({
      where: { userId: 'EMP1001' },
      select: ['siteCode'],
    });
  });
});
