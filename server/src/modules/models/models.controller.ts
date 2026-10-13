import { Controller, Get, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { ModelsService } from './models.service';
import { ModelManifestResponseDto } from './dto/model-manifest-response.dto';

@ApiTags('models')
@Controller('models')
export class ModelsController {
  constructor(private readonly modelsService: ModelsService) {}

  @Get('manifest')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Fetch ONNX face model download manifest' })
  @ApiResponse({
    status: 200,
    description: 'Model manifest list',
    type: ModelManifestResponseDto,
  })
  getModelManifest(): ModelManifestResponseDto {
    return this.modelsService.getModelManifest();
  }
}
