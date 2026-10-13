import { Injectable } from '@nestjs/common';
import {
  ModelManifestResponseDto,
  ModelManifestItemDto,
} from './dto/model-manifest-response.dto';

@Injectable()
export class ModelsService {
  private getBaseUrl(): string {
    if (process.env.MODELS_BASE_URL) {
      return process.env.MODELS_BASE_URL.replace(/\/+$/, '');
    }
    const hostUrl = process.env.BASE_URL ? process.env.BASE_URL.replace(/\/+$/, '') : 'http://localhost:3000';
    return `${hostUrl}/uploads/models`;
  }

  private readonly modelDefinitions = [
    {
      name: 'det_10g',
      file_name: 'det_10g.onnx',
      size_bytes: 4305997,
      sha256: '5cad2a01da24944e1b08129e4f46bf24e7a00f5905bc4972332bb41119a38ee3',
    },
    {
      name: '1k3d68',
      file_name: '1k3d68.onnx',
      size_bytes: 36268296,
      sha256: 'f36aba1bd169421883b19acad22596fd5c038ea64f81af4db038293c554379ee',
    },
    {
      name: 'w600k_r50',
      file_name: 'w600k_r50.onnx',
      size_bytes: 43788579,
      sha256: '5219952187162894d4d4578c480f279f4a88625d62a210c29101c918d146ca83',
    },
  ];

  getModelManifest(): ModelManifestResponseDto {
    const baseUrl = this.getBaseUrl();
    const models: ModelManifestItemDto[] = this.modelDefinitions.map((m) => ({
      ...m,
      download_url: `${baseUrl}/${m.file_name}`,
    }));

    const total_size_bytes = models.reduce(
      (sum, item) => sum + item.size_bytes,
      0,
    );

    return {
      models,
      total_size_bytes,
    };
  }
}
