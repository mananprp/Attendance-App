import { Test, TestingModule } from '@nestjs/testing';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';

describe('UsersController', () => {
  let controller: UsersController;
  let usersService: any;

  const mockUser = { userId: 'EMP1001', name: 'Alice Sharma' };
  const mockFile = { originalname: 'test.jpg', buffer: Buffer.from('abc') } as any;

  beforeEach(async () => {
    usersService = {
      uploadPhotoV1: jest.fn().mockResolvedValue({
        version: 1,
        photo_url: '/uploads/photos/EMP1001_v1.jpg',
        photo_change_used: false,
      }),
      replacePhotoV2: jest.fn().mockResolvedValue({
        version: 2,
        photo_url: '/uploads/photos/EMP1001_v2.jpg',
        photo_change_used: true,
      }),
      getActivePhoto: jest.fn().mockResolvedValue({
        version: 1,
        photo_url: '/uploads/photos/EMP1001_v1.jpg',
        uploaded_at: '2026-09-30T09:00:00.000Z',
        hash: 'mockhash',
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [UsersController],
      providers: [{ provide: UsersService, useValue: usersService }],
    }).compile();

    controller = module.get<UsersController>(UsersController);
  });

  it('should upload photo v1', async () => {
    const result = await controller.uploadPhotoV1(mockUser, mockFile);
    expect(result.version).toBe(1);
    expect(usersService.uploadPhotoV1).toHaveBeenCalledWith('EMP1001', mockFile);
  });

  it('should replace photo v2', async () => {
    const result = await controller.replacePhotoV2(mockUser, mockFile);
    expect(result.version).toBe(2);
    expect(result.photo_change_used).toBe(true);
    expect(usersService.replacePhotoV2).toHaveBeenCalledWith('EMP1001', mockFile);
  });

  it('should get active photo', async () => {
    const result = await controller.getActivePhoto(mockUser);
    expect(result.version).toBe(1);
    expect(usersService.getActivePhoto).toHaveBeenCalledWith('EMP1001');
  });
});
