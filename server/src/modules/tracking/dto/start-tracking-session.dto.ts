import { IsNotEmpty, IsNumber, IsString } from 'class-validator';

export class StartTrackingSessionDto {
  @IsNotEmpty()
  @IsString()
  sessionId: string;

  @IsNotEmpty()
  @IsNumber()
  startTimeUtc: number;
}
