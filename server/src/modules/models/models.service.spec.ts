import { Test, TestingModule } from '@nestjs/testing';
import { ModelsService } from './models.service';

describe('ModelsService', () => {
  let service: ModelsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ModelsService],
    }).compile();

    service = module.get<ModelsService>(ModelsService);
  });

  it('should return model manifest with 3 required ONNX models', () => {
    const manifest = service.getModelManifest();
    expect(manifest.models).toBeDefined();
    expect(manifest.models.length).toBe(3);

    const modelNames = manifest.models.map((m) => m.name);
    expect(modelNames).toContain('det_10g');
    expect(modelNames).toContain('1k3d68');
    expect(modelNames).toContain('w600k_r50');

    expect(manifest.total_size_bytes).toBeGreaterThan(0);
    manifest.models.forEach((m) => {
      expect(m.file_name).toBeDefined();
      expect(m.size_bytes).toBeGreaterThan(0);
      expect(m.sha256).toBeDefined();
      expect(m.download_url).toBeDefined();
    });
  });
});
