import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class LoginRequestDto {
  @ApiProperty({ example: 'EMP1001', description: 'Unique user identifier' })
  @IsString()
  @IsNotEmpty()
  user_id: string;

  @ApiProperty({ example: 'Password@123', description: 'User password' })
  @IsString()
  @IsNotEmpty()
  password: string;
}
