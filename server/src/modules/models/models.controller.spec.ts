import { Test, TestingModule } from '@nestjs/testing';
import { ModelsController } from './models.controller';
import { ModelsService } from './models.service';

describe('ModelsController', () => {
  let controller: ModelsController;
  let service: any;

  beforeEach(async () => {
    service = {
      getModelManifest: jest.fn().mockReturnValue({
        models: [
          {
            name: 'det_10g',
            file_name: 'det_10g.onnx',
            size_bytes: 17825792,
            sha256: '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08',
            download_url: 'https://models.attendance.company.com/det_10g.onnx',
          },
        ],
        total_size_bytes: 17825792,
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [ModelsController],
      providers: [{ provide: ModelsService, useValue: service }],
    }).compile();

    controller = module.get<ModelsController>(ModelsController);
  });

  it('should return model manifest', () => {
    const result = controller.getModelManifest();
    expect(result.models.length).toBe(1);
    expect(result.models[0].name).toBe('det_10g');
    expect(service.getModelManifest).toHaveBeenCalled();
  });
});
