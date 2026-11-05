import { Test, TestingModule } from '@nestjs/testing';
import { HttpStatus } from '@nestjs/common';
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
        photo_url: '/v1/users/me/photo/file',
        photo_change_used: false,
      }),
      replacePhotoV2: jest.fn().mockResolvedValue({
        version: 2,
        photo_url: '/v1/users/me/photo/file',
        photo_change_used: true,
      }),
      getActivePhoto: jest.fn().mockResolvedValue({
        version: 1,
        photo_url: '/v1/users/me/photo/file',
        uploaded_at: '2026-09-30T09:00:00.000Z',
        hash: 'mockhash123',
      }),
      getActivePhotoFile: jest.fn().mockResolvedValue({
        data: Buffer.from('photo_bytes'),
        mimeType: 'image/jpeg',
        hash: 'mockhash123',
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
    expect(result.photo_url).toBe('/v1/users/me/photo/file');
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
    expect(result.photo_url).toBe('/v1/users/me/photo/file');
    expect(usersService.getActivePhoto).toHaveBeenCalledWith('EMP1001');
  });

  describe('getActivePhotoFile', () => {
    it('should return 200 with photo bytes, ETag, and Cache-Control headers', async () => {
      const mockReq = { headers: {} } as any;
      const setHeader = jest.fn();
      const send = jest.fn();
      const status = jest.fn().mockReturnValue({ send });
      const mockRes = { setHeader, status, send } as any;

      await controller.getActivePhotoFile(mockUser, mockReq, mockRes);

      expect(usersService.getActivePhotoFile).toHaveBeenCalledWith('EMP1001');
      expect(setHeader).toHaveBeenCalledWith('ETag', '"mockhash123"');
      expect(setHeader).toHaveBeenCalledWith(
        'Cache-Control',
        'private, max-age=0, must-revalidate',
      );
      expect(setHeader).toHaveBeenCalledWith('Content-Type', 'image/jpeg');
      expect(status).toHaveBeenCalledWith(HttpStatus.OK);
      expect(send).toHaveBeenCalledWith(Buffer.from('photo_bytes'));
    });

    it('should return 304 Not Modified when If-None-Match matches ETag', async () => {
      const mockReq = { headers: { 'if-none-match': '"mockhash123"' } } as any;
      const setHeader = jest.fn();
      const send = jest.fn();
      const status = jest.fn().mockReturnValue({ send });
      const mockRes = { setHeader, status, send } as any;

      await controller.getActivePhotoFile(mockUser, mockReq, mockRes);

      expect(status).toHaveBeenCalledWith(HttpStatus.NOT_MODIFIED);
      expect(send).toHaveBeenCalled();
    });
  });
});
