import {
  Controller,
  Post,
  Put,
  Get,
  UseGuards,
  UseInterceptors,
  UploadedFile,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiConsumes,
  ApiBody,
} from '@nestjs/swagger';
import { FileInterceptor } from '@nestjs/platform-express';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import {
  CurrentUser,
  AuthenticatedUser,
} from '../../common/decorators/current-user.decorator';
import { UsersService } from './users.service';
import {
  PhotoUploadResponseDto,
  PhotoDetailResponseDto,
} from './dto/photo-response.dto';

@ApiTags('users')
@Controller('users')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post('me/photo')
  @HttpCode(HttpStatus.CREATED)
  @UseInterceptors(FileInterceptor('photo'))
  @ApiConsumes('multipart/form-data')
  @ApiOperation({ summary: 'Upload onboarding profile photo (Version 1)' })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['photo'],
      properties: {
        photo: {
          type: 'string',
          format: 'binary',
          description: 'Profile image (JPEG/PNG)',
        },
      },
    },
  })
  @ApiResponse({
    status: 201,
    description: 'Photo v1 uploaded successfully',
    type: PhotoUploadResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'Invalid image or photo v1 already uploaded',
  })
  async uploadPhotoV1(
    @CurrentUser() user: AuthenticatedUser,
    @UploadedFile() file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    return this.usersService.uploadPhotoV1(user.userId, file);
  }

  @Put('me/photo')
  @HttpCode(HttpStatus.OK)
  @UseInterceptors(FileInterceptor('photo'))
  @ApiConsumes('multipart/form-data')
  @ApiOperation({ summary: 'Replace profile photo (Version 2)' })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['photo'],
      properties: {
        photo: {
          type: 'string',
          format: 'binary',
          description: 'New profile image (JPEG/PNG)',
        },
      },
    },
  })
  @ApiResponse({
    status: 200,
    description: 'Photo v2 replaced successfully',
    type: PhotoUploadResponseDto,
  })
  @ApiResponse({
    status: 400,
    description: 'Photo v1 missing or invalid image',
  })
  @ApiResponse({
    status: 403,
    description: 'Photo change already used (one-time replacement rule violation)',
  })
  async replacePhotoV2(
    @CurrentUser() user: AuthenticatedUser,
    @UploadedFile() file: Express.Multer.File,
  ): Promise<PhotoUploadResponseDto> {
    return this.usersService.replacePhotoV2(user.userId, file);
  }

  @Get('me/photo')
  @ApiOperation({ summary: 'Get active profile photo' })
  @ApiResponse({
    status: 200,
    description: 'Active photo details',
    type: PhotoDetailResponseDto,
  })
  @ApiResponse({ status: 404, description: 'No photo found' })
  async getActivePhoto(
    @CurrentUser() user: AuthenticatedUser,
  ): Promise<PhotoDetailResponseDto> {
    return this.usersService.getActivePhoto(user.userId);
  }
}
